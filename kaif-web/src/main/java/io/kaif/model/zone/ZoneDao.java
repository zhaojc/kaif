package io.kaif.model.zone;

import static java.util.stream.Collectors.*;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import io.kaif.database.DaoOperations;
import io.kaif.model.account.Authority;

@Repository
public class ZoneDao implements DaoOperations {

  @Autowired
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private final RowMapper<ZoneInfo> zoneInfoMapper = (rs, n) -> {
    return new ZoneInfo(rs.getString("zone"),
        rs.getString("aliasName"),
        rs.getString("theme"),
        Authority.valueOf(rs.getString("readAuthority")),
        Authority.valueOf(rs.getString("writeAuthority")),
        convertUuidArray(rs.getArray("adminAccountIds")).collect(toList()),
        rs.getTimestamp("createTime").toInstant());
  };

  @Override
  public NamedParameterJdbcTemplate namedJdbc() {
    return namedParameterJdbcTemplate;
  }

  public ZoneInfo create(String zone,
      String aliasName,
      String theme,
      Authority read,
      Authority write,
      Instant now) {
    ZoneInfo zoneInfo = ZoneInfo.create(zone, aliasName, theme, read, write, now);

    jdbc().update(""
            + " INSERT "
            + "   INTO ZoneInfo "
            + "        (zone, aliasName, theme, readAuthority, writeAuthority, "
            + "         createTime, adminAccountIds) "
            + " VALUES "
            + questions(7),
        zoneInfo.getZone(),
        zoneInfo.getAliasName(),
        zoneInfo.getTheme(),
        zoneInfo.getReadAuthority().name(),
        zoneInfo.getWriteAuthority().name(),
        Timestamp.from(zoneInfo.getCreateTime()),
        createUuidArray(zoneInfo.getAdminAccountIds().stream()));
    return zoneInfo;
  }

  public ZoneInfo getZoneWithoutCache(String zone) {
    return jdbc().queryForObject("SELECT * FROM ZoneInfo WHERE zone = ? ", zoneInfoMapper, zone);
  }
}
