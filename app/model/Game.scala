package model

import java.util.UUID

import play.api.libs.json._
import scala.language.implicitConversions

object Answer {
  implicit val format = Json.format[Answer]
}

case class Answer(id: String, answer: String, points: Int)

object GameQuestion {
  implicit val format = Json.format[GameQuestion]
}

case class GameQuestion(question: String, answers: Vector[Answer])

object Game {
  implicit val gameFormat: Format[Game] = Json.format[Game]
}

case class Game(id: String, title: String, description: String, questions: Vector[GameQuestion])

object GameInstance {
  implicit val format = Json.format[GameInstance]
}

case class GameInstance(id: String, game: Game, moderator: String, players: Map[String, Int])

object Games {

  implicit class AnswerBuilder(answer: String) {
    def isWorth(points: Int) = Answer(UUID.randomUUID().toString, answer, points)

    def |(points: Int) = isWorth(points)
  }

  implicit class GameQuestionBuilder(question: String) {
    def hasAnswers(answers: Answer*) = GameQuestion(question, answers.toVector)
  }

  implicit def game(id: String) = new GameTitleBuilder(id)

  class GameTitleBuilder(id: String) {
    def withTitle(t: String) = new GameDescriptionBuilder(id, t)
  }

  class GameDescriptionBuilder(val id: String, val title: String) {
    def withDescription(description: String) = new GameQuestionsBuilder(this, description)
  }

  class GameQuestionsBuilder(gdb: GameDescriptionBuilder, description: String) {
    def hasQuestions(questions: Vector[GameQuestion]) = Game(gdb.id, gdb.title, description, questions)
  }

  val ScalaQuiz = game("scala") withTitle "Scala Quiz" withDescription "A simple Scala quiz" hasQuestions Vector(
    "A mutable variable is declared with the keyword" hasAnswers(
        "var" | 10,
        "val" | 0,
        "let" | 0
      ),
    "Which collection type does not allow duplicates?" hasAnswers(
      "List" | 0,
      "Map" | 0,
      "Set" | 10
      ),
    "Scala allows only one class to be defined in a .scala file." hasAnswers(
      "true" | 0,
      "false" | 10
      )
  )

  val GeneralQuiz = game("general") withTitle "General Quiz" withDescription "A mixed subject quiz" hasQuestions Vector(
    "The world's first 'ski-thru' fast food joint is located in Sweden. Which company operates the 'ski-thru' restaurant?" hasAnswers(
      "Arby's" | 0,
      "McDonalds" | 7,
      "Harveys" | 0,
      "Subway" | 0
    ),
    "Which of the following is an anise-flavoured spirit from Greece?" hasAnswers(
      "pastis" | 0,
      "sambucco" | 0,
      "absinthe" | 0,
      "ouzo" | 7
    ),
    "Which of the following limes would NOT be used for cooking?" hasAnswers(
      "cork lime" | 7,
      "key lime" | 0,
      "kaffir lime" | 0,
      "tahiti lime" | 0
    )
  )

  val JsQuiz = game("jsquiz") withTitle "JavaScript Quiz" withDescription "JavaScript Quiz" hasQuestions Vector(
    "Inside which HTML element do we put the JavaScript?" hasAnswers(
      "<script>" | 10,
      "<javascript>" | 0,
      "<scripting>" | 0,
      "<js>" | 0
      ),

    """What is the correct JavaScript syntax to write "Hello World"?""" hasAnswers(
      """document.write("Hello World")""" | 10,
      """("Hello World")""" | 0,
      """"Hello World"""" | 0,
      """response.write("Hello World")""" | 0
      ),

    "Where is the correct place to insert a JavaScript?" hasAnswers(
      "The <body> section" | 0,
      "Both the <head> section and the <body> section are correct" | 10,
      "The <head> section" | 0
      ),

    """What is the correct syntax for referring to an external script called "xxx.js"?""" hasAnswers(
      """<script type="text/javascript" src="xxx.js">""" | 10,
      """<script type="text/javascript" name="xxx.js">""" | 0,
      """<script type="text/javascript" href="xxx.js">""" | 0
      ),

    "The external JavaScript file must contain the <script> tag" hasAnswers(

      """True """ | 0,
      """False """ | 10
      ),

    """How do you write "Hello World" in an alert box?"""" hasAnswers(

      """alert("Hello World") """ | 10,
      """alertBox="Hello World" """ | 0,
      """alertBox("Hello World") """ | 0,
      """msgBox("Hello World") """ | 0
      ),

    """How do you create a function?""" hasAnswers(

      """function myFunction() """ | 10,
      """function=myFunction() """ | 0,
      """function:myFunction() """ | 0
      ),

    """How do you call a function named "myFunction"?""" hasAnswers(

      """call myFunction() """ | 0,
      """myFunction() """ | 10,
      """call function myFunction """ | 0
      ),

    """How do you write a conditional statement for executing some code if "i" is equal to 5?""" hasAnswers(

      """if (i==5) """ | 10,
      """if i=5 then """ | 0,
      """if i==5 then """ | 0,
      """if i=5 """ | 0
      ),

    """How do you write a conditional statement for executing some code if "i" is NOT equal to 5?""" hasAnswers(

      """if (i != 5) """ | 10,
      """if =! 5 then """ | 0,
      """if <>5 """ | 0,
      """if (i <> 5) """ | 0
      ),

    """How does a "while" loop start?""" hasAnswers(

      """while i=1 to 10 """ | 0,
      """while (i<=10;i++) """ | 0,
      """while (i<=10)""" | 10
      ),

    """How does a "for" loop start?""" hasAnswers(

      """for (i = 0; i <= 5; i++) """ | 10,
      """for i = 1 to 5 """ | 0,
      """for (i = 0; i <= 5) """ | 0,
      """for (i <= 5; i++) """ | 0
      ),

    """How can you add a comment in a JavaScript?""" hasAnswers(
      """'This is a comment """ | 0,
      """<!--This is a comment--> """ | 0,
      """//This is a comment """ | 10
      ),

    """What is the correct JavaScript syntax to insert a comment that has more than one line?""" hasAnswers(
      """//This comment has\nmore than one line// """ | 0,
      """/*This comment has\nmore than one line*/ """ | 10,
      """<!--This comment has\nmore than one line--> """ | 0
      ),

    """What is the correct way to write a JavaScript array?""" hasAnswers(

      """var txt = new Array(1:"tim",2:"kim",3:"jim") """ | 0,
      """var txt = new Array:1=("tim")2=("kim")3=("jim") """ | 0,
      """var txt = new Array("tim","kim","jim") """ | 10,
      """var txt = new Array="tim","kim","jim"""" | 0
      ),

    """How do you round the number 7.25, to the nearest integer?""" hasAnswers(
      """rnd(7.25) """ | 0,
      """Math.rnd(7.25) """ | 0,
      """round(7.25) """ | 0,
      """Math.round(7.25) """ | 10
      ),

    """How do you find the number with the highest value of x and y?""" hasAnswers(
      """Math.ceil(x,y)""" | 0,
      """top(x,y)""" | 0,
      """Math.max(x,y)""" | 10,
      """ceil(x,y)""" | 0
      ),

    """What is the correct JavaScript syntax for opening a new window called "w2" ?""" hasAnswers(

      """w2=window.open("http://www.w3schools.com"); """ | 10,
      """w2=window.new("http://www.w3schools.com");  """ | 0
      ),

    """How do you put a message in the browser's status bar?""" hasAnswers(

      """window.status = "put your message here" """ | 0,
      """statusbar = "put your message here" """ | 0,
      """status("put your message here") """ | 0,
      """window.status("put your message here") """ | 10
      ),

    """How can you find a client's browser name?""" hasAnswers(

      """navigator.appName """ | 10,
      """client.navName """ | 0,
      """browser.name """ | 0
      )
  )

  val AllGames = Set(ScalaQuiz, GeneralQuiz, JsQuiz)


}
