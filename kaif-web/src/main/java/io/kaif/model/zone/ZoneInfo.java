package io.kaif.model.zone;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import io.kaif.model.account.Authority;

public class ZoneInfo {

  public static final String THEME_DEFAULT = "z-theme-default";

  // theme used in site related zone, like Blog or FAQ
  public static final String THEME_KAIF = "z-theme-kaif";

  /**
   * - must start with az09, end with az09, no dash
   * - must use dash to separate
   * - 3~30 chars.
   * - not allow concat multiple dash (use code to validate, not regex)
   */
  private static final Pattern ZONE_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9\\-]{1,28}[a-z0-9]$");

  /**
   * fallback to valid zone name whenever possible (user is easily typo)
   * <p>
   * fallback rule is follow valid zone pattern
   */
  public static String zoneFallback(String rawZone) {
    if (Strings.isNullOrEmpty(rawZone)) {
      return "";
    }
    return rawZone.toLowerCase().replaceAll("[\\-_]+", "-");
  }

  public static ZoneInfo createKaif(String zone, String aliasName, Instant now) {
    Preconditions.checkArgument(validateZone(zone));
    boolean allowDownVote = true;
    boolean hideFromTopRanking = true;
    return new ZoneInfo(zone,
        aliasName,
        THEME_KAIF,
        Authority.CITIZEN,
        Authority.SYSOP,
        Collections.emptyList(),
        allowDownVote,
        hideFromTopRanking,
        now);
  }

  public static ZoneInfo createDefault(String zone, String aliasName, Instant now) {
    Preconditions.checkArgument(validateZone(zone));
    boolean allowDownVote = true;
    boolean hideFromTopRanking = false;
    return new ZoneInfo(zone,
        aliasName,
        THEME_DEFAULT,
        Authority.CITIZEN,
        Authority.CITIZEN,
        Collections.emptyList(),
        allowDownVote,
        hideFromTopRanking,
        now);
  }

  private static boolean validateZone(String zone) {
    return zone != null && ZONE_PATTERN.matcher(zone).matches() && !zone.contains("--");
  }

  /**
   * zone are always lowercase and URL friendly
   */
  private final String zone;

  /**
   * display name of zone, may include Upper case or even Chinese
   */
  private final String aliasName;

  /**
   * css theme class name
   */
  private final String theme;

  /**
   * which authority can vote this zone
   */
  private final Authority voteAuthority;

  /**
   * which authority can write article in this zone
   */
  private final Authority writeAuthority;

  /**
   * accountId can do everything about this zone, he ignore all authority check
   */
  private final List<UUID> adminAccountIds;

  /**
   * specify the zone is up vote only
   */
  private final boolean allowDownVote;

  /**
   * hide this zone in home page top ranking
   */
  private final boolean hideFromTop;

  private final Instant createTime;

  ZoneInfo(String zone,
      String aliasName,
      String theme,
      Authority voteAuthority,
      Authority writeAuthority,
      List<UUID> adminAccountIds,
      boolean allowDownVote,
      boolean hideFromTop,
      Instant createTime) {
    this.zone = zone;
    this.aliasName = aliasName;
    this.theme = theme;
    this.voteAuthority = voteAuthority;
    this.writeAuthority = writeAuthority;
    this.adminAccountIds = adminAccountIds;
    this.allowDownVote = allowDownVote;
    this.hideFromTop = hideFromTop;
    this.createTime = createTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ZoneInfo zoneInfo = (ZoneInfo) o;

    if (zone != null ? !zone.equals(zoneInfo.zone) : zoneInfo.zone != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return zone != null ? zone.hashCode() : 0;
  }

  public String getZone() {
    return zone;
  }

  public String getAliasName() {
    return aliasName;
  }

  public Authority getVoteAuthority() {
    return voteAuthority;
  }

  public String getTheme() {
    return theme;
  }

  public Authority getWriteAuthority() {
    return writeAuthority;
  }

  public List<UUID> getAdminAccountIds() {
    return adminAccountIds;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public boolean canUpVote(UUID accountId, Set<Authority> authorities) {
    if (adminAccountIds.contains(accountId)) {
      return true;
    }
    return authorities.contains(voteAuthority);
  }

  public boolean canDownVote(UUID accountId, Set<Authority> authorities) {
    if (!allowDownVote) {
      return false;
    }
    if (adminAccountIds.contains(accountId)) {
      return true;
    }
    return authorities.contains(voteAuthority);
  }

  public boolean canWriteArticle(UUID accountId, Set<Authority> authorities) {
    if (adminAccountIds.contains(accountId)) {
      return true;
    }
    return authorities.contains(writeAuthority);
  }

  public boolean isAllowDownVote() {
    return allowDownVote;
  }

  public boolean isHideFromTop() {
    return hideFromTop;
  }

  public ZoneInfo withAdmins(List<UUID> accountIds) {
    return new ZoneInfo(zone,
        aliasName,
        theme,
        voteAuthority,
        writeAuthority,
        ImmutableList.copyOf(accountIds),
        allowDownVote,
        hideFromTop,
        createTime);
  }

  public ZoneInfo withAllowDownVote(boolean allow) {
    return new ZoneInfo(zone,
        aliasName,
        theme,
        voteAuthority,
        writeAuthority,
        adminAccountIds,
        allow,
        hideFromTop,
        createTime);
  }
}
