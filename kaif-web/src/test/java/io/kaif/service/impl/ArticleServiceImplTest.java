package io.kaif.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import io.kaif.flake.FlakeId;
import io.kaif.model.account.Account;
import io.kaif.model.account.AccountStats;
import io.kaif.model.article.Article;
import io.kaif.model.article.ArticleContentType;
import io.kaif.model.article.ArticleLinkType;
import io.kaif.model.debate.Debate;
import io.kaif.model.debate.DebateContentType;
import io.kaif.model.debate.DebateDao;
import io.kaif.model.zone.Zone;
import io.kaif.model.zone.ZoneInfo;
import io.kaif.service.AccountService;
import io.kaif.test.DbIntegrationTests;
import io.kaif.web.support.AccessDeniedException;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ArticleServiceImplTest extends DbIntegrationTests {

  @Autowired
  private ArticleServiceImpl service;

  @Autowired
  private AccountService accountService;

  @Autowired
  private DebateDao debateDao;

  private ZoneInfo zoneInfo;
  private Article article;
  private Account citizen;

  @Before
  public void setUp() throws Exception {
    zoneInfo = savedZoneDefault("pic");
    citizen = savedAccountCitizen("citizen1");
    article = savedArticle(zoneInfo, citizen, "art-1");
  }

  @Test
  public void debate() throws Exception {
    Account debater = savedAccountCitizen("debater1");
    Debate created = service.debate(zoneInfo.getZone(),
        article.getArticleId(),
        Debate.NO_PARENT,
        debater.getAccountId(),
        "pixel art is better");

    Debate debate = debateDao.findDebate(article.getArticleId(), created.getDebateId()).get();
    assertEquals(DebateContentType.MARK_DOWN, debate.getContentType());
    assertEquals("debater1", debate.getDebaterName());
    assertEquals(debater.getAccountId(), debate.getDebaterId());
    assertFalse(debate.hasParent());
    assertFalse(debate.isMaxLevel());
    assertEquals(1, debate.getLevel());
    assertEquals("pixel art is better", debate.getContent());
    assertEquals(0L, debate.getDownVote());
    assertEquals(0L, debate.getUpVote());
    assertNotNull(debate.getCreateTime());
    assertNotNull(debate.getLastUpdateTime());

    assertEquals(1,
        service.findArticle(zoneInfo.getZone(), article.getArticleId()).get().getDebateCount());

    assertEquals(1, accountService.loadAccountStats(debater.getAccountId()).getDebateCount());
  }

  @Test
  public void debate_escape_content() throws Exception {
    Account debater = savedAccountCitizen("debater1");
    Debate created = service.debate(zoneInfo.getZone(),
        article.getArticleId(),
        Debate.NO_PARENT,
        debater.getAccountId(),
        "pixel art is better<evil>hi</evil>");

    Debate debate = debateDao.findDebate(article.getArticleId(), created.getDebateId()).get();
    assertEquals(DebateContentType.MARK_DOWN, debate.getContentType());
    assertEquals("pixel art is better&lt;evil&gt;hi&lt;/evil&gt;", debate.getContent());
  }

  @Test
  public void listHotDebates_one_level() throws Exception {
    Zone zone = zoneInfo.getZone();
    FlakeId articleId = article.getArticleId();
    assertEquals(0, service.listHotDebates(zone, articleId, 0).size());

    List<Debate> debates = IntStream.rangeClosed(1, 3)
        .mapToObj(i -> service.debate(zone,
            articleId,
            Debate.NO_PARENT,
            citizen.getAccountId(),
            "debate-content-" + i))
        .collect(toList());

    assertEquals(debates, service.listHotDebates(zone, articleId, 0));
  }

  @Test
  public void listHotDebates_tree() throws Exception {
    Debate d1 = savedDebate(null);
    Debate d1_1 = savedDebate(d1);
    Debate d1_1_1 = savedDebate(d1_1);
    Debate d2 = savedDebate(null);
    Debate d2_1 = savedDebate(d2);
    Debate d2_2 = savedDebate(d2);

    // begin out or order
    Debate d1_1_2 = savedDebate(d1_1);
    Debate d3 = savedDebate(null);
    Debate d2_1_1 = savedDebate(d2_1);
    Debate d3_1 = savedDebate(d3);
    Debate d1_2 = savedDebate(d1);

    List<Debate> expect = asList(//
        d1, d1_1, d1_1_1, d1_1_2, d1_2, //
        d2, d2_1, d2_1_1, d2_2, //
        d3, d3_1);

    assertEquals(expect, service.listHotDebates(zoneInfo.getZone(), article.getArticleId(), 0));
  }

  private Debate savedDebate(Debate parent) {
    return service.debate(zoneInfo.getZone(),
        article.getArticleId(),
        Optional.ofNullable(parent).map(Debate::getDebateId).orElse(Debate.NO_PARENT),
        citizen.getAccountId(),
        "debate-content-" + Math.random());
  }

  @Test
  public void debate_max_level() throws Exception {
    Account debater = savedAccountCitizen("debater1");
    FlakeId parentId = Debate.NO_PARENT;
    Debate last = null;
    for (int i = 0; i < 10; i++) {
      last = service.debate(zoneInfo.getZone(),
          article.getArticleId(),
          parentId,
          debater.getAccountId(),
          "nested");
      parentId = last.getDebateId();
    }
    assertTrue(last.isMaxLevel());
    try {
      service.debate(zoneInfo.getZone(),
          article.getArticleId(),
          parentId,
          debater.getAccountId(),
          "failed");
      fail("IllegalArgumentException expected");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void debate_reply() throws Exception {
    Account debater = savedAccountCitizen("debater1");
    Debate l1 = service.debate(zoneInfo.getZone(),
        article.getArticleId(),
        Debate.NO_PARENT,
        debater.getAccountId(),
        "pixel art is better");
    Debate l2 = service.debate(zoneInfo.getZone(),
        article.getArticleId(),
        l1.getDebateId(),
        debater.getAccountId(),
        "i think so");
    assertEquals(2, l2.getLevel());
    assertTrue(l2.hasParent());
    assertTrue(l2.isParent(l1));
    assertFalse(l1.isParent(l2));

    assertEquals(2,
        service.findArticle(zoneInfo.getZone(), article.getArticleId()).get().getDebateCount());
    Debate l3 = service.debate(zoneInfo.getZone(),
        article.getArticleId(),
        l2.getDebateId(),
        debater.getAccountId(),
        "no no no");

    assertEquals(3, l3.getLevel());
    assertTrue(l3.hasParent());
    assertTrue(l3.isParent(l2));
    assertFalse(l2.isParent(l3));

    assertEquals(3,
        service.findArticle(zoneInfo.getZone(), article.getArticleId()).get().getDebateCount());
  }

  @Test
  public void debate_not_enough_authority() throws Exception {
    ZoneInfo zoneRequireCitizen = savedZoneDefault("fun");
    Article article = savedArticle(zoneRequireCitizen, citizen, "fun-no1");
    Account tourist = savedAccountTourist("notActivated");
    try {
      service.debate(zoneRequireCitizen.getZone(),
          article.getArticleId(),
          Debate.NO_PARENT,
          tourist.getAccountId(),
          "pixel art is better");
      fail("AccessDeniedException expected");
    } catch (AccessDeniedException expected) {
    }
  }

  @Test
  public void listNewArticles() throws Exception {
    Account author = savedAccountCitizen("citizen");
    ZoneInfo fooZone = savedZoneDefault("foo");
    Article a1 = service.createExternalLink(author.getAccountId(),
        fooZone.getZone(),
        "title1",
        "http://foo1.com");
    Article a2 = service.createExternalLink(author.getAccountId(),
        fooZone.getZone(),
        "title2",
        "http://foo2.com");
    Article a3 = service.createExternalLink(author.getAccountId(),
        fooZone.getZone(),
        "title2",
        "http://foo2.com");

    assertEquals(asList(a3, a2, a1), service.listLatestArticles(fooZone.getZone(), 0));
  }

  @Test
  public void createExternalLink_escape_content() throws Exception {
    Article created = service.createExternalLink(citizen.getAccountId(),
        zoneInfo.getZone(),
        "title1<script>alert('123');</script>",
        "http://foo.com<script>alert('123');</script>");
    Article article = service.findArticle(created.getZone(), created.getArticleId()).get();
    assertEquals("title1&lt;script&gt;alert(&#39;123&#39;);&lt;/script&gt;", article.getTitle());
    assertEquals("http://foo.com&lt;script&gt;alert(&#39;123&#39;);&lt;/script&gt;",
        article.getContent());
  }

  @Test
  public void createExternalLink() throws Exception {
    Article created = service.createExternalLink(citizen.getAccountId(),
        zoneInfo.getZone(),
        "title1",
        "http://foo.com");
    Article article = service.findArticle(created.getZone(), created.getArticleId()).get();
    assertEquals(zoneInfo.getZone(), article.getZone());
    assertEquals("title1", article.getTitle());
    assertNull(article.getUrlName());
    assertNotNull(article.getCreateTime());
    assertEquals("http://foo.com", article.getContent());
    assertEquals(ArticleContentType.URL, article.getContentType());
    assertEquals(ArticleLinkType.EXTERNAL, article.getLinkType());
    assertEquals(citizen.getUsername(), article.getAuthorName());
    assertEquals(citizen.getAccountId(), article.getAuthorId());
    assertFalse(article.isDeleted());
    assertEquals(0, article.getUpVote());
    assertEquals(0, article.getDownVote());
    assertEquals(0, article.getDebateCount());

    AccountStats stats = accountService.loadAccountStats(citizen.getAccountId());
    assertEquals(1, stats.getArticleCount());
  }

  @Test
  public void createExternalLink_not_enough_authority() throws Exception {
    ZoneInfo zoneRequireCitizen = savedZoneDefault("fun");
    Account tourist = savedAccountTourist("notActivated");
    try {
      service.createExternalLink(tourist.getAccountId(),
          zoneRequireCitizen.getZone(),
          "title1",
          "http://foo.com");
      fail("AccessDeniedException expected");
    } catch (AccessDeniedException expected) {
    }
  }
}