package io.kaif.web.v1;

import static java.util.stream.Collectors.*;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.wordnik.swagger.annotations.Api;

import io.kaif.flake.FlakeId;
import io.kaif.model.clientapp.ClientAppScope;
import io.kaif.model.clientapp.ClientAppUserAccessToken;
import io.kaif.model.debate.Debate;
import io.kaif.model.zone.Zone;
import io.kaif.service.ArticleService;
import io.kaif.web.v1.dto.V1DebateDto;

@Api(value = "debate", description = "Debates on articles")
@RestController
@RequestMapping(value = "/v1/debate", produces = MediaType.APPLICATION_JSON_VALUE)
public class V1DebateResource {

  static class CreateDebateEntry {
    @NotNull
    public FlakeId articleId;

    public FlakeId parentDebateId;

    @Size(min = Debate.CONTENT_MIN, max = Debate.CONTENT_MAX)
    @NotNull
    public String content;
  }

  static class UpdateDebateEntry {
    @Size(min = Debate.CONTENT_MIN, max = Debate.CONTENT_MAX)
    @NotNull
    public String content;
  }

  @Autowired
  private ArticleService articleService;

  @RequiredScope(ClientAppScope.PUBLIC)
  @RequestMapping(value = "/{debateId}", method = RequestMethod.GET)
  public V1DebateDto debate(ClientAppUserAccessToken accessToken,
      @PathVariable("debateId") FlakeId debateId) {
    return articleService.loadDebateWithCache(debateId).toV1Dto();
  }

  //TODO document large of data
  @RequiredScope(ClientAppScope.PUBLIC)
  @RequestMapping(value = "/article/{articleId}/tree", method = RequestMethod.GET)
  public void debateTreeOfArticle(ClientAppUserAccessToken accessToken,
      @PathVariable("articleId") FlakeId articleId) {
  }

  @RequiredScope(ClientAppScope.PUBLIC)
  @RequestMapping(value = "/latest", method = RequestMethod.GET)
  public List<V1DebateDto> latest(ClientAppUserAccessToken accessToken,
      @RequestParam(value = "start-debate-id", required = false) FlakeId startDebateId) {
    return toDtos(articleService.listLatestDebates(startDebateId));
  }

  private List<V1DebateDto> toDtos(List<Debate> debates) {
    return debates.stream().map(Debate::toV1Dto).collect(toList());
  }

  @RequiredScope(ClientAppScope.PUBLIC)
  @RequestMapping(value = "/zone/{zone}/latest", method = RequestMethod.GET)
  public List<V1DebateDto> latestByZone(ClientAppUserAccessToken accessToken,
      @PathVariable("zone") String zone,
      @RequestParam(value = "start-debate-id", required = false) FlakeId startDebateId) {
    return toDtos(articleService.listLatestZoneDebates(Zone.valueOf(zone), startDebateId));
  }

  @RequiredScope(ClientAppScope.DEBATE)
  @RequestMapping(value = "/user/{username}/submitted", method = RequestMethod.GET)
  public List<V1DebateDto> userSubmitted(ClientAppUserAccessToken accessToken,
      @PathVariable("username") String username,
      @RequestParam(value = "start-debate-id", required = false) FlakeId startDebateId) {
    return toDtos(articleService.listDebatesByDebater(username, startDebateId));
  }

  @ResponseStatus(HttpStatus.CREATED)
  @RequiredScope(ClientAppScope.DEBATE)
  @RequestMapping(value = "", method = RequestMethod.PUT, consumes = {
      MediaType.APPLICATION_JSON_VALUE })
  public void create(ClientAppUserAccessToken accessToken,
      @Valid @RequestBody CreateDebateEntry entry) {

  }

  @RequiredScope(ClientAppScope.DEBATE)
  @RequestMapping(value = "/{debateId}/content", method = RequestMethod.POST, consumes = {
      MediaType.APPLICATION_JSON_VALUE })
  public void updateDebateContent(ClientAppUserAccessToken accessToken,
      @PathVariable("debateId") FlakeId debateId,
      @Valid @RequestBody UpdateDebateEntry entry) {

  }

}
