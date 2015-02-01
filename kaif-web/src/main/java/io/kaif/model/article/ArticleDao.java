package io.kaif.model.article;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import io.kaif.database.DaoOperations;
import io.kaif.flake.FlakeId;

@Repository
public class ArticleDao implements DaoOperations {

  @Autowired
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

  private final RowMapper<Article> articleMapper = (rs, rowNum) -> {
    return new Article(//
        rs.getString("zone"),
        FlakeId.valueOf(rs.getLong("articleId")),
        rs.getString("title"),
        rs.getString("urlName"),
        ArticleLinkType.valueOf(rs.getString("linkType")),
        rs.getTimestamp("createTime").toInstant(),
        rs.getString("content"),
        ArticleContentType.valueOf(rs.getString("contentType")),
        UUID.fromString(rs.getString("authorId")),
        rs.getString("authorName"),
        rs.getBoolean("deleted"),
        rs.getLong("upVote"),
        rs.getLong("downVote"));
  };

  @Override
  public NamedParameterJdbcTemplate namedJdbc() {
    return namedParameterJdbcTemplate;
  }

  public Article createArticle(Article article) {
    jdbc().update(""
            + " INSERT "
            + "   INTO Article "
            + "        (zone, articleid, title, urlname, linktype, createtime, content, "
            + "         contenttype, authorid, authorname, deleted, upvote, downvote)"
            + " VALUES "
            + questions(13),
        article.getZone(),
        article.getArticleId().value(),
        article.getTitle(),
        article.getUrlName(),
        article.getLinkType().name(),
        Timestamp.from(article.getCreateTime()),
        article.getContent(),
        article.getContentType().name(),
        article.getAuthorId(),
        article.getAuthorName(),
        article.isDeleted(),
        article.getUpVote(),
        article.getDownVote());
    return article;
  }

  public Optional<Article> findArticle(String zone, FlakeId articleId) {
    final String sql = " SELECT * FROM Article WHERE zone = ? AND articleId = ? LIMIT 1 ";
    return jdbc().query(sql, articleMapper, zone, articleId.value()).stream().findAny();
  }

}
