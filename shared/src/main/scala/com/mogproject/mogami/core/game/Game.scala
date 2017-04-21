package com.mogproject.mogami.core.game

import com.mogproject.mogami.core.Ptype.PAWN
import com.mogproject.mogami.core.{Player, Square}
import com.mogproject.mogami.core.state.{State, StateCache}
import com.mogproject.mogami.core.state.StateCache.Implicits._
import com.mogproject.mogami.core.state.StateHash.StateHash
import com.mogproject.mogami.core.io._
import com.mogproject.mogami.core.move._
import com.mogproject.mogami.util.Implicits._

import scala.util.{Failure, Success, Try}

/**
  * Game
  */
case class Game(initialState: State = State.HIRATE,
                moves: Vector[Move] = Vector.empty,
                gameInfo: GameInfo = GameInfo(),
                movesOffset: Int = 0,
                finalAction: Option[SpecialMove] = None,
                givenHistory: Option[Vector[State]] = None
               )(implicit stateCache: StateCache) extends CsaGameWriter with SfenLike with KifGameWriter {

  require(history.length == moves.length + 1, "all moves must be valid")

  import com.mogproject.mogami.core.game.Game.GameStatus._

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: Game =>
      // ignore givenHistory
      initialState == that.initialState &&
        moves == that.moves &&
        gameInfo == that.gameInfo &&
        movesOffset == that.movesOffset &&
        finalAction == that.finalAction
    case _ => false
  }

  override def hashCode(): Int = (((initialState.hashCode * 31 + moves.hashCode) * 31 + gameInfo.hashCode) * 31 + movesOffset.hashCode) * 31 + finalAction.hashCode

  /** history of states */
  lazy val history: Vector[State] = {
    givenHistory.getOrElse(moves.scanLeft(Some(initialState): Option[State])((s, m) => s.flatMap(_.makeMove(m))).flatten)
  }

  lazy val hashCodes: Vector[StateHash] = history.map(_.hash)

  lazy val status: GameStatus = {
    finalAction match {
      case Some(IllegalMove(_)) => IllegallyMoved
      case Some(Resign(_)) => Resigned
      case Some(TimeUp(_)) => TimedUp
      case _ =>
        if (currentState.isMated) {
          if (lastMove.exists(m => m.isCheck && m.isDrop && m.oldPtype == PAWN))
            Uchifuzume
          else
            Mated
        } else if (isPerpetualCheck) {
          PerpetualCheck
        } else if (isRepetition) {
          Drawn // Sennichite
        } else {
          Playing
        }
    }
  }

  /**
    * Get the latest state.
    */
  def currentState: State = history.last

  def lastMove: Option[Move] = moves.lastOption

  def lastMoveTo: Option[Square] = moves.lastOption.map(_.to)

  def turn: Player = currentState.turn

  def makeMove(move: Move): Option[Game] = {
    (status == Playing && currentState.isValidMove(move)).option(this.copy(moves = moves :+ move, givenHistory = currentState.makeMove(move).map(history :+ _)))
  }

  def makeMove(move: MoveBuilder): Option[Game] = move.toMove(currentState, lastMoveTo).flatMap(makeMove)

  override def toSfenString: String = (initialState.toSfenString :: movesOffset.toString :: moves.map(_.toSfenString).toList).mkString(" ")

  /**
    * Check if the latest move is the repetition.
    *
    * @return true if the latest move is the repetition
    */
  def isRepetition: Boolean = hashCodes.drop(1).count(_ == currentState.hash) >= 4

  def isPerpetualCheck: Boolean = currentState.isChecked &&
    (history.drop(1).reverse.takeWhile(s => s.turn == !turn || s.isChecked).count(_.hash == currentState.hash) >= 4)

  /**
    * Moves for description. This includes an illegal move if it exists.
    *
    * @return vector of moves for description
    */
  def descriptiveMoves: Vector[Move] = finalAction match {
    case Some(IllegalMove(mv)) => moves :+ mv
    case _ => moves
  }
}

object Game extends CsaGameReader with SfenFactory[Game] with KifGameReader {

  override def parseSfenString(s: String): Game = {
    val tokens = s.split(" ")
    if (tokens.length < 4) throw new RecordFormatException(1, s"there must be four or more tokens: ${s}")

    val st = State.parseSfenString(tokens.take(3).mkString(" "))
    val offset = Try(tokens(3).toInt) match {
      case Success(n) => n
      case Failure(e) => throw new RecordFormatException(1, s"offset must be number: ${tokens(3)}")
    }
    val gi = GameInfo()
    // initialize without information
    val moves = tokens.drop(4).map(MoveBuilderSfen.parseSfenString)
    moves.foldLeft[Game](Game(st, Vector.empty, gi, offset)) { (g, m) =>
      g.makeMove(m).getOrElse(throw new RecordFormatException(1, s"invalid move: ${m.toSfenString}"))
    }
  }

  object GameStatus {

    sealed trait GameStatus

    case object Playing extends GameStatus

    case object Mated extends GameStatus

    case object Uchifuzume extends GameStatus

    case object PerpetualCheck extends GameStatus

    case object Drawn extends GameStatus

    case object TimedUp extends GameStatus

    case object IllegallyMoved extends GameStatus

    case object Resigned extends GameStatus

  }

}
