package io.kaif.service;

import java.util.List;

import javax.annotation.Nullable;

import io.kaif.flake.FlakeId;
import io.kaif.model.account.Authorization;
import io.kaif.model.article.Article;
import io.kaif.model.vote.ArticleVoter;
import io.kaif.model.vote.DebateVoter;
import io.kaif.model.vote.VoteState;

public interface VoteService {

  void voteArticle(VoteState newState,
      FlakeId articleId,
      Authorization authorization,
      VoteState previousState,
      long previousCount);

  List<DebateVoter> listDebateVoters(Authorization voter, FlakeId articleId);

  void voteDebate(VoteState newState,
      FlakeId debateId,
      Authorization voter,
      VoteState previousState,
      long previousCount);

  List<ArticleVoter> listArticleVoters(Authorization voter, List<FlakeId> articleIds);

  List<DebateVoter> listDebateVotersByIds(Authorization voter, List<FlakeId> debateIds);

  List<Article> listUpVotedArticles(Authorization voter, @Nullable FlakeId startArticleId);
}
