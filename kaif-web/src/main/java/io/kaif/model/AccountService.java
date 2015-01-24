package io.kaif.model;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import io.kaif.mail.MailAgent;
import io.kaif.model.account.Account;
import io.kaif.model.account.AccountAccessToken;
import io.kaif.model.account.AccountAuth;
import io.kaif.model.account.AccountDao;
import io.kaif.model.account.AccountSecret;
import io.kaif.model.account.Authority;

@Service
@Transactional
public class AccountService {

  private static final Duration ACCOUNT_TOKEN_EXPIRE = Duration.ofDays(30);
  @Autowired
  private AccountDao accountDao;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @Autowired
  private AccountSecret accountSecret;

  @Autowired
  private MailAgent mailAgent;

  public Account createViaEmail(String name, String email, String password) {
    Preconditions.checkArgument(Account.isValidPassword(password));
    Preconditions.checkArgument(Account.isValidName(name));
    Preconditions.checkNotNull(email);
    return accountDao.create(name, email, passwordEncoder.encode(password));
  }

  @VisibleForTesting
  Account findById(String accountId) {
    return accountDao.findById(UUID.fromString(accountId)).get();
  }

  public Optional<AccountAuth> authenticate(String name, String password) {
    return accountDao.findByName(name)
        .filter(account -> passwordEncoder.matches(password, account.getPasswordHash()))
        .map(this::createAccountAuth);
    //TODO not activate treat as fail
  }

  //TODO activate via email
  private AccountAuth createAccountAuth(Account account) {
    Instant expireTime = Instant.now().plus(ACCOUNT_TOKEN_EXPIRE);
    String accessToken = new AccountAccessToken(account.getAccountId(),
        account.getPasswordHash(),
        account.getAuthorities()).encode(expireTime, accountSecret);
    return new AccountAuth(account.getName(), accessToken, expireTime.toEpochMilli());
  }

  /**
   * the verification go against database, so it is slow. using {@link
   * io.kaif.model.account.AccountAccessToken#tryDecode(String, io.kaif.model.account.AccountSecret)}
   * if you want faster check.
   */
  public Optional<AccountAccessToken> verifyAccessToken(String rawAccessToken) {
    return AccountAccessToken.tryDecode(rawAccessToken, accountSecret)
        .filter(token -> verifyTokenToAccount(token).isPresent());
  }

  private Optional<Account> verifyTokenToAccount(AccountAccessToken token) {
    return accountDao.findById(token.getAccountId()).filter(account -> {
      // verify database already change password or authorities
      return token.matches(account.getPasswordHash(), account.getAuthorities());
    });
  }

  public AccountAuth extendsAccessToken(AccountAccessToken accessToken) {
    return Optional.ofNullable(accessToken)
        .flatMap(token -> accountDao.findById(token.getAccountId()))
        .map(this::createAccountAuth)
        .get();
  }

  public boolean isNameAvailable(String name) {
    return !accountDao.findByName(name).isPresent();
  }

  public boolean isEmailAvailable(String email) {
    return accountDao.isEmailAvailable(email);
  }

  public void updateAuthorities(String accountId, EnumSet<Authority> authorities) {
    accountDao.updateAuthorities(UUID.fromString(accountId), authorities);
  }

  public void updatePassword(String accountId, String password) {
    Preconditions.checkArgument(Account.isValidPassword(password));
    accountDao.updatePasswordHash(UUID.fromString(accountId), passwordEncoder.encode(password));
  }
}
