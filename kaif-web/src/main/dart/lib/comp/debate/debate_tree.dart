library debate_tree;

import 'dart:html';
import 'package:kaif_web/util.dart';
import 'package:kaif_web/model.dart';
import '../vote/votable.dart';
import '../article/article-list.dart';
import 'debate_form.dart';
import 'dart:async';

class DebateTree {

  final Element elem;
  final ArticleService articleService;
  final VoteService voteService;
  final AccountSession accountSession;

  DebateTree(this.elem, this.articleService, this.voteService, this.accountSession) {

    var zone = (elem.querySelector('[name=zoneInput]') as HiddenInputElement).value;
    var articleElem = elem.querySelector('[article]');
    ArticleComp articleComp = new ArticleComp(articleElem, voteService, accountSession, zone);

    _initArticleVote(articleComp);

    var articleId = articleComp.articleId;
    elem.querySelectorAll('[debate-form]').forEach((el) {
      new DebateForm.placeHolder(el, articleService);
    });

    List<DebateComp> debateComps = elem.querySelectorAll('[debate]').map((el) {
      return new DebateComp(el, articleService, voteService, accountSession, zone, articleId);
    }).toList();

    _initDebateVoters(debateComps, articleId);
  }

  void _initArticleVote(ArticleComp articleComp) {
    Future<List<ArticleVoter>> future;
    if (accountSession.isSignIn) {
      future = voteService.listArticleVotersInRange(articleComp.articleId, articleComp.articleId);
    } else {
      future = new Future.value([]);
    }
    future.then((voters) {
      articleComp.voteBox.applyVoters(voters);
    });
  }

  void _initDebateVoters(List<DebateComp> debateComps, String articleId) {
    List<DebateVoteBox> voteBoxes = debateComps.map((comp) => comp.voteBox).toList();

    Future<List<DebateVoter>> future;
    if (accountSession.isSignIn) {
      future = voteService.listDebateVoters(articleId);
    } else {
      future = new Future.value([]);
    }

    future.then((voters) {
      voteBoxes.forEach((box) => box.applyVoters(voters));
    });
  }
}

class DebateComp {
  final VoteService voteService;
  final AccountSession accountSession;
  final ArticleService articleService;
  final Element elem;
  final String zone;
  final String articleId;
  String debateId;

  DebateVoteBox voteBox;

  DebateComp(this.elem, this.articleService, this.voteService, this.accountSession, this.zone,
             this.articleId) {
    debateId = elem.dataset['debate-id'];

    var voteElem = elem.querySelector('[debate-vote-box]');
    var voteCountElem = elem.querySelector('[debate-vote-count]');
    voteBox = new DebateVoteBox(voteElem, this, voteCountElem);

    var replierElem = elem.querySelector('[debate-replier]');
    new DebateReplier(replierElem, articleService, debateId);
  }
}

class DebateReplier {
  final Element elem;
  final ArticleService articleService;
  final String debateId;

  DebateForm form;
  bool _opened = false;

  toggleShow() {
    if (_opened) {
      form.elem.remove();
    } else {
      elem.parent.append(form.elem);
    }
    _opened = !_opened;
  }

  DebateReplier(this.elem, this.articleService, this.debateId) {
    elem.onClick.listen(_onClick);
  }

  void _onClick(Event e) {
    e
      ..preventDefault()
      ..stopPropagation();

    //lazy create
    if (form == null) {
      Element placeHolderElem = new DivElement();
      elem.append(placeHolderElem);

      form = new DebateForm.placeHolder(placeHolderElem, articleService)
        ..parentDebateId = debateId;
    }

    toggleShow();
  }
}

class DebateVoteBox extends Votable {

  final DebateComp debateComp;

  DebateVoteBox(Element elem, this.debateComp, Element voteCountElem)
  : super(elem) {
    var upVoteElem = elem.querySelector('[debate-up-vote]');
    var downVoteElem = elem.querySelector('[debate-down-vote]');
    var currentCount = int.parse(elem.dataset['debate-vote-count']);
    init(currentCount, upVoteElem, downVoteElem, voteCountElem);
  }

  void applyVoters(List<DebateVoter> voters) {
    if (!debateComp.accountSession.isSignIn) {
      applyNotSignIn();
      return;
    }

    var voter = voters
    .firstWhere((voter) => voter.debateId == debateComp.debateId, orElse:() => null);
    if (voter == null) {
      applyNoVoter();
      return;
    }

    applyVoterReady(voter);
  }

  Future onVote(VoteState newState, VoteState previousState, int previousCount) {
    return debateComp.voteService.voteDebate(
        newState, debateComp.zone, debateComp.articleId, debateComp.debateId, previousState,
        previousCount);
  }

}