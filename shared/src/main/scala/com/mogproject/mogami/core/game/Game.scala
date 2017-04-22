package com.mogproject.mogami.core.game

import com.mogproject.mogami.core.game.Game.{BranchNo, GamePosition}
import com.mogproject.mogami.core.game.GameStatus.GameStatus
import com.mogproject.mogami.core.state.StateCache.Implicits.DefaultStateCache
import com.mogproject.mogami.core.state.{State, StateCache}
import com.mogproject.mogami.core.io._
import com.mogproject.mogami.core.move._
import com.mogproject.mogami.util.Implicits._

/**
  * Game
  */
case class Game(trunk: Branch = Branch(),
                branches: Vector[Branch] = Vector.empty,
                gameInfo: GameInfo = GameInfo()
               )(implicit val stateCache: StateCache) extends CsaGameWriter with SfenGameWriter with KifGameWriter {

  private[this] def getBranch(branchNo: BranchNo): Option[Branch] =
    (1 <= branchNo && branchNo <= branches.length).option(branches(branchNo - 1))

  def createBranch(position: GamePosition, move: Move): Option[(Game, BranchNo)] = ???

  def deleteBranch(branchNo: BranchNo): Option[Game] = ???

  def makeMove(move: Move, branchNo: BranchNo = 0): Option[Game] =
    if (branchNo == 0) {
      trunk.makeMove(move).map(tr => this.copy(trunk = tr))
    } else {
      for {
        br <- getBranch(branchNo)
        index = branchNo - 1
        nxt <- br.makeMove(move)
      } yield {
        this.copy(branches = branches.updated(index, nxt))
      }
    }

  // aliases to the trunk
  def moves: Vector[Move] = trunk.moves

  def lastState: State = trunk.lastState

  def status: GameStatus = trunk.status

  /**
    * Get all moves from the trunk's start position
    *
    * @param branchNo branch number (trunk:0)
    */
  def getAllMoves(branchNo: BranchNo): Vector[Move] = if (branchNo == 0) {
    trunk.moves
  } else {
    getBranch(branchNo).map { br => trunk.moves.take(br.offset - trunk.offset) ++ br.moves }.getOrElse(Vector.empty)
  }

  def hasComment(gamePosition: GamePosition): Boolean = if (gamePosition.isTrunk) {
    trunk.hasComment(gamePosition.position)
  } else {
    getBranch(gamePosition.branch).exists(br =>
      if (gamePosition.position <= br.offset) trunk.hasComment(gamePosition.position) else br.hasComment(gamePosition.position)
    )
  }

  def getComment(gamePosition: GamePosition): Option[String] = if (gamePosition.isTrunk) {
    trunk.comments.get(gamePosition.position)
  } else {
    getBranch(gamePosition.branch).flatMap(br =>
      (if (gamePosition.position <= br.offset) trunk else br).comments.get(gamePosition.position)
    )
  }

  def getFinalAction(branchNo: BranchNo): Option[SpecialMove] = if (branchNo == 0) {
    trunk.finalAction
  } else {
    getBranch(branchNo).flatMap(_.finalAction)
  }
}

object Game extends CsaGameReader with SfenGameReader with KifGameReader {

  type BranchNo = Int // branch number: root = 0

  case class GamePosition(branch: BranchNo, position: Int) {
    def isTrunk: Boolean = branch == 0
  }

}