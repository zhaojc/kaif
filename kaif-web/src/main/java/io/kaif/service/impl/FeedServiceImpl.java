package io.kaif.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.kaif.flake.FlakeId;
import io.kaif.model.account.Authorization;
import io.kaif.model.feed.FeedAsset;
import io.kaif.model.feed.FeedAssetDao;
import io.kaif.service.FeedService;

@Service
public class FeedServiceImpl implements FeedService {

  private static final int FEED_PAGE_SIZE = 25;

  @Autowired
  private FeedAssetDao feedAssetDao;

  @Override
  public FeedAsset createReplyFeed(FlakeId debateId, UUID replyToAccountId) {
    return feedAssetDao.insertFeed(FeedAsset.createReply(debateId,
        replyToAccountId,
        Instant.now()));
  }

  @Override
  public List<FeedAsset> listFeeds(Authorization authorization, FlakeId startAssetId) {
    return feedAssetDao.listFeedsDesc(authorization.authenticatedId(),
        startAssetId,
        FEED_PAGE_SIZE);
  }
}
