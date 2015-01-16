package actors

import akka.actor._
import model.{GameQuestion, GameInstance}
import play.api.Logger
import play.api.libs.json.Json

case class NextQuestionResponse(state: String, question: Option[GameQuestion], scores: Map[String, Int])

object NextQuestionResponse {
  implicit val format = Json.format[NextQuestionResponse]
}

object GameActor {
  case class RegisterPlayer(player: String)
  case object NextQuestion
  case object CloseQuestion
  case class StartGame(instance: GameInstance)
  case object PlayerJoined
  case class AnswerQuestion(player: String, answer: String)
  case object AnswerAccepted
  case object GetQuestion
  case class InvalidState(msg: String)
  case class AddEventSubscriber(ref: ActorRef)
  case class RemoveEventSubscriber(ref: ActorRef)
  case class GameStarted(id: String)
  case object GameEnded

  sealed trait State
  case object NotStarted extends State
  case object WaitingForPlayers extends State
  case object PlayerRegistered extends State
  case object QuestionOpen extends State
  case object ReviewingQuestion extends State
  case object GameOver extends State

  case class PlayerJoinedEvent(instance: GameInstance)
  case class QuestionOpenEvent(currentQuestion: GameQuestion, instance: GameInstance)
  case class ReviewingQuestionEvent(currentQuestion: GameQuestion, instance: GameInstance)
  case class GameOverEvent(instance: GameInstance)
}

sealed trait Data
case object Uninitialized extends Data
case class InitializedGame(instance: GameInstance) extends Data
case class GameInProgress(instance: GameInstance, currentQuestion: Int, playerAnswers: Map[String, String]) extends Data {
  def question = instance.game.questions(currentQuestion)
}
case class CompletedGame(instance: GameInstance) extends Data

class GameActor() extends Actor with FSM[GameActor.State, Data] {
  import GameActor._

  startWith(NotStarted, Uninitialized)
  when(NotStarted) {
    case Event(GameActor.StartGame(instance: GameInstance), Uninitialized) =>
      Logger.debug(s"Game instance ${instance.id} set with game ${instance.game.toString.take(200)}")
      goto (WaitingForPlayers) using InitializedGame(instance) replying(GameStarted(instance.id))
  }


  var eventSubscribers: Set[ActorRef] = Set.empty

  def notifySubscribers(event: AnyRef) = for (subscriber <- eventSubscribers) subscriber ! event

  when(WaitingForPlayers) {
    case Event(GameActor.RegisterPlayer(player), InitializedGame(instance)) =>
      Logger.debug(s"player $player has joined!")
      val newgame = instance.copy(players = instance.players + (player -> 0))
      notifySubscribers(PlayerJoinedEvent(newgame))
      goto (PlayerRegistered) using InitializedGame(newgame) replying PlayerJoined
  }

  when(PlayerRegistered) {
    case Event(GameActor.RegisterPlayer(player), InitializedGame(instance)) =>
      val newGame = instance.copy(players = instance.players + (player -> 0))
      Logger.debug(s"player $player has joined!")
      notifySubscribers(PlayerJoinedEvent(newGame))
      goto (PlayerRegistered) using InitializedGame(newGame) replying PlayerJoined
    case Event(GameActor.NextQuestion, InitializedGame(instance)) =>
      Logger.debug(s"current question is : ${instance.game.questions.head}")
      val newGame = GameInProgress(instance, 0, Map.empty)
      notifySubscribers(QuestionOpenEvent(newGame.question, newGame.instance))
      val initPlayerScores = for {
        (player, previousScore) <- newGame.instance.players
      } yield (player, 0)
      goto (QuestionOpen) using newGame replying(NextQuestionResponse(QuestionOpen.toString, Some(newGame.question), initPlayerScores ))
  }

  when(QuestionOpen) {
    case Event(GameActor.AnswerQuestion(player, answer), game: GameInProgress) =>
      Logger.debug(s"player $player answered $answer to current question")
      stay using game.copy(playerAnswers = game.playerAnswers + (player -> answer)) replying(AnswerAccepted)
    case Event(GameActor.GetQuestion, game: GameInProgress) =>
      Logger.debug("sending back current question")
      stay replying(game.question)
    case Event(GameActor.RegisterPlayer(player), game: GameInProgress) =>  //TODO: consider removing later
      log.info(s"request to register $player while game has started!")
      stay replying(if (game.instance.players.contains(player)) PlayerJoined else InvalidState)
    case Event(GameActor.NextQuestion, game @ GameInProgress(instance, _, playerAnswers)) =>
      Logger.debug(s"Closing question ${game.currentQuestion} on game ${game.instance.id}")
      val newPlayerScores = for {
        (player, previousScore) <- game.instance.players
        playerAnswer <- game.playerAnswers.get(player).orElse(Some(""))
        answerScore <- game.question.answers.find(_.id == playerAnswer).map(_.points).orElse(Some(0))
      } yield (player, previousScore + answerScore)
      val newGame = game.copy(instance = instance.copy(players = newPlayerScores))
      Logger.debug(s"player scores: $newPlayerScores")
      notifySubscribers(ReviewingQuestionEvent(newGame.question, newGame.instance))
      goto (ReviewingQuestion) using newGame replying NextQuestionResponse(ReviewingQuestion.toString, Some(newGame.question), newPlayerScores)
  }

  when(ReviewingQuestion) {
    case Event(GameActor.NextQuestion, game: GameInProgress) if game.currentQuestion >= game.instance.game.questions.length - 1 =>
      Logger.debug(s"all questions asked!")
      val newGame = CompletedGame(game.instance)
      notifySubscribers(GameOverEvent(newGame.instance))
      goto (GameOver) using newGame replying(NextQuestionResponse(GameOver.toString, None, game.instance.players))
    case Event(GameActor.NextQuestion, game: GameInProgress) =>
      Logger.debug(s"current question is : ${game.instance.game.questions(game.currentQuestion + 1)}")
      val newGame =  game.copy(currentQuestion = game.currentQuestion + 1)
      notifySubscribers(QuestionOpenEvent(newGame.question, newGame.instance))
      goto (QuestionOpen) using newGame replying(NextQuestionResponse(QuestionOpen.toString, Some(game.instance.game.questions(game.currentQuestion + 1)), newGame.instance.players))
    case Event(GameActor.RegisterPlayer(player), game: GameInProgress) =>  //TODO: consider removing later
      log.info(s"request to register $player while game has started!")
      stay replying(if (game.instance.players.contains(player)) PlayerJoined else InvalidState)

  }

  when(GameOver) {
    case Event(_, game: CompletedGame) =>
      sender ! game.instance
      stay
  }

  whenUnhandled {
    case Event(AddEventSubscriber(subscriber), _) =>
      eventSubscribers = eventSubscribers + subscriber
      stay
    case Event(RemoveEventSubscriber(subscriber), _) =>
      eventSubscribers = eventSubscribers - subscriber
      stay
    case Event(msg, _) =>
      Logger.warn(s"Unhandled message in Game actor at state $stateName: $msg")
      sender ! InvalidState(s"Message $msg is invalid in state $stateName")
      stay
  }

  initialize()

  override def preStart = {
    super.preStart
    Logger.debug("Game actor starting " + self.path )
  }
}
