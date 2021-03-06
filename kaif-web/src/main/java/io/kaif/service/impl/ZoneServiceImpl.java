package io.kaif.service.impl;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.kaif.model.account.Account;
import io.kaif.model.account.AccountDao;
import io.kaif.model.account.Authority;
import io.kaif.model.account.Authorization;
import io.kaif.model.article.ArticleDao;
import io.kaif.model.exception.CreditNotEnoughException;
import io.kaif.model.zone.Zone;
import io.kaif.model.zone.ZoneDao;
import io.kaif.model.zone.ZoneInfo;
import io.kaif.service.ZoneService;
import io.kaif.util.Try;
import io.kaif.web.support.AccessDeniedException;

@Service
@Transactional
public class ZoneServiceImpl implements ZoneService {

  private static final int HONOR_SCORE_PER_ZONE = 10;

  private static final int MAX_AVAILABLE_ZONE = 5;

  @Autowired
  private ZoneDao zoneDao;

  @Autowired
  private ArticleDao articleDao;

  @Autowired
  private AccountDao accountDao;

  @Override
  public ZoneInfo loadZone(Zone zone) {
    return zoneDao.loadZoneWithCache(zone);
  }

  @Override
  public ZoneInfo createDefault(String zone, String aliasName) {
    return zoneDao.create(ZoneInfo.createDefault(zone, aliasName, Instant.now()));
  }

  @Override
  public ZoneInfo createKaif(String zone, String aliasName) {
    return zoneDao.create(ZoneInfo.createKaif(zone, aliasName, Instant.now()));
  }

  @Override
  public void updateTheme(Zone zone, String theme) {
    zoneDao.updateTheme(zone, theme);
  }

  @Override
  public Map<String, List<ZoneInfo>> listZoneAtoZ() {
    Function<ZoneInfo, String> capitalizeFirstChar = zoneInfo -> zoneInfo.getZone()
        .value()
        .substring(0, 1)
        .toUpperCase();
    return zoneDao.listOrderByName()
        .stream()
        .collect(Collectors.groupingBy(capitalizeFirstChar, LinkedHashMap::new, toList()));
  }

  @Override
  public List<ZoneInfo> listRecommendZones() {
    int recommendSize = 10;
    List<ZoneInfo> results = articleDao.listHotZonesWithCache(recommendSize,
        Instant.now().minus(Duration.ofHours(24)));
    if (results.size() >= recommendSize) {
      return results;
    }
    // not enough hot zone within 24 hours, fall back to all zones...
    List<ZoneInfo> all = zoneDao.listOrderByName()
        .stream()
        .filter(zoneInfo -> !zoneInfo.isHideFromTop())
        .collect(toList());
    Collections.shuffle(all);
    return all.stream().limit(recommendSize).collect(toList());
  }

  @Override
  public List<ZoneInfo> listCitizenZones() {
    return zoneDao.listOrderByName()
        .stream()
        .filter(zoneInfo -> zoneInfo.getWriteAuthority() == Authority.CITIZEN)
        .collect(toList());
  }

  @Override
  public ZoneInfo createByUser(String zone, String aliasName, Authorization creator)
      throws CreditNotEnoughException {

    Account account = verifyAuthority(creator).flatMap(this::verifyCredit).get();

    ZoneInfo zoneInfo = ZoneInfo.createDefault(zone, aliasName, Instant.now())
        .withAdmins(singletonList(account.getAccountId()));

    zoneDao.create(zoneInfo);
    return zoneInfo;
  }

  @Override
  public List<ZoneInfo> listAdministerZones(String username) {
    Account account = accountDao.loadByUsername(username);
    return zoneDao.listZonesByAdmin(account.getAccountId());
  }

  @Override
  public boolean isZoneAvailable(String zone) {
    return Zone.isValid(zone) && ZoneInfo.isValidDefault(zone) && !zoneDao.findZoneWithoutCache(Zone
        .valueOf(zone)).isPresent();
  }

  @Override
  public boolean canCreateZone(Authorization authorization) {
    return verifyAuthority(authorization).flatMap(this::verifyCredit).isSuccess();
  }

  /**
   * @return username of administrators
   */
  @Override
  public List<String> listAdministratorsWithCache(Zone zone) {
    return zoneDao.listAdministratorsWithCache(zone);
  }

  private Try<Account> verifyAuthority(Authorization authorization) {
    // won't compile if put `supplier` inline
    Supplier<Account> supplier = () -> accountDao.strongVerifyAccount(authorization)
        .filter(account -> account.containsAuthority(Authority.CITIZEN))
        .orElseThrow(() -> new AccessDeniedException("no permission to create zone"));

    return Try.apply(supplier);
  }

  private Try<Account> verifyCredit(Account account) {
    return Try.apply(() -> {
      int zones = zoneDao.listZonesByAdmin(account.getAccountId()).size();
      if (zones >= MAX_AVAILABLE_ZONE) {
        throw new CreditNotEnoughException();
      }
      int requireScore = (zones + 1) * HONOR_SCORE_PER_ZONE;
      if (accountDao.loadStats(account.getUsername()).getHonorScore() < requireScore) {
        throw new CreditNotEnoughException();
      }
      return account;
    });
  }

}
