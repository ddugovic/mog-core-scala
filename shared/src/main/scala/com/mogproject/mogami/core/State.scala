package com.mogproject.mogami.core

import com.mogproject.mogami._
import com.mogproject.mogami.core.io._
import com.mogproject.mogami.util.MapUtil
import com.mogproject.mogami.util.Implicits._

/**
  * State class
  */
case class State(turn: Player, board: BoardType, hand: HandType) extends CsaLike with SfenLike {

  require(checkCapacity, "the number of pieces must be within the capacity")
  require(hand.keySet == State.EMPTY_HANDS.keySet, "hand pieces must be in-hand type")
  require(!board.keySet.contains(HAND), "all board pieces must have on-board squares")
  require(board.forall{ case (s, p) => s.isLegalZone(p) }, "all board pieces must be placed in their legal zones")
  require(!getKing(!turn).exists(getAttackBB(turn).get), "player must not be able to attack the opponent's king")

  import com.mogproject.mogami.core.State.PromotionFlag.{PromotionFlag, CannotPromote, CanPromote, MustPromote}

  override def toCsaString: String = {
    val boardString = (1 to 9).map { rank =>
      (9 to 1 by -1).map { file => board.get(Square(file, rank)).map(_.toCsaString).getOrElse(" * ") }.mkString(s"P$rank", "", "")
    }.mkString("\n")

    val handString = Player.constructor.map { p =>
      s"P${p.toCsaString}" + Ptype.inHand.map { pt => s"00${pt.toCsaString}" * hand.getOrElse(Piece(p, pt), 0) }.mkString
    }.mkString("\n")

    Seq(boardString, handString, turn.toCsaString).mkString("\n")
  }

  override def toSfenString: String = {
    def stringifyNumber(n: Int, threshold: Int = 0): String = (n <= threshold).fold("", n.toString)

    val boardString = (1 to 9).map { rank =>
      val (ss, nn) = (9 to 1 by -1).map { file =>
        board.get(Square(file, rank))
      }.foldLeft(("", 0)) {
        case ((s, n), Some(p)) => (s + stringifyNumber(n) + p.toSfenString, 0)
        case ((s, n), None) => (s, n + 1)
      }
      ss + stringifyNumber(nn)
    }.mkString("/")

    val handString = hand.filter(_._2 != 0).toSeq.sorted.map { case (p, n) => stringifyNumber(n, 1) + p.toSfenString }.mkString

    s"$boardString ${turn.toSfenString} ${handString.isEmpty.fold("-", handString)}"
  }

  def getPromotionFlag(from: Square, to: Square): Option[PromotionFlag] = {
    if (from.isHand) {
      Some(CannotPromote)
    } else {
      for (p <- board.get(from) if p.owner == turn) yield {
        (p.ptype.isPromoted, from.isPromotionZone(turn) || to.isPromotionZone(turn), to.isLegalZone(p)) match {
          case (false, true, true) => CanPromote
          case (false, true, false) => MustPromote
          case _ => CannotPromote
        }
      }
    }
  }

  /**
    * Occupancy bitboards
    */
  private[this] def aggregateSquares(boardMap: BoardType): BitBoard = boardMap.keys.view.map(BitBoard.ident).fold(BitBoard.empty)(_ | _)

  private[this] lazy val occupancyAll: BitBoard = aggregateSquares(board)

  private[this] lazy val occupancyByOwner: Map[Player, BitBoard] = board.groupBy(_._2.owner).mapValues(aggregateSquares)

  private[this] lazy val occupancyByPiece: Map[Piece, BitBoard] = board.groupBy(_._2).mapValues(aggregateSquares)

  def occupancy: BitBoard = occupancyAll

  def occupancy(player: Player): BitBoard = occupancyByOwner.getOrElse(player, BitBoard.empty)

  def occupancy(piece: Piece): BitBoard = occupancyByPiece.getOrElse(piece, BitBoard.empty)

  def getSquares(piece: Piece): Set[Square] = occupancy(piece).toSet

  def getKing(player: Player): Option[Square] = occupancy(Piece(player, KING)).toList.headOption

  lazy val turnsKing: Option[Square] = getKing(turn)

  def getRangedPieces(player: Player): Seq[(Square, Piece)] = board.filter { case (s, p) => p.owner == player && p.isRanged }.toSeq

  /**
    * Attack bitboards
    */
  lazy val attackBBOnBoard: Map[Player, Map[Square, BitBoard]] = {
    val m = (for ((sq, piece) <- board) yield {
      (piece.owner, sq) -> Attack.get(piece, sq, occupancy, occupancy(Piece(piece.owner, PAWN)))
    }).filter(_._2.nonEmpty).groupBy(_._1._1)

    Map(BLACK -> Map.empty[Square, BitBoard], WHITE -> Map.empty[Square, BitBoard]) ++ m.mapValues {
      _.map { case ((_, s), b) => s -> b }
    }
  }

  lazy val attackBBInHand: Map[(Square, Piece), BitBoard] = for {
    (piece, num) <- hand if piece.owner == turn && num > 0
  } yield {
    (HAND, piece) -> Attack.get(piece, HAND, occupancy, occupancy(Piece(turn, PAWN)))
  }

  def getAttackBB(player: Player): BitBoard = attackBBOnBoard(player).values.fold(BitBoard.empty)(_ | _)

  /**
    * Get the positions of pieces that are attacking the turn player's king
    *
    * @return set of squares
    */
  lazy val attackers: Set[Square] = turnsKing.map(k => attackBBOnBoard(!turn).filter(_._2.get(k)).keys.toSet).getOrElse(Set.empty)

  /**
    * Get the attackers' potential attack bitboard (assuming that there is no obstacles)
    */
  lazy val attackerPotentialBB: BitBoard = attackers.map(sq => Attack.get(board(sq), sq, BitBoard.empty, BitBoard.empty)).fold(BitBoard.empty)(_ | _)

  /**
    * Get the guard pieces, which protect the turn player's king from ranged attack.
    *
    * @return set of squares and guarding area bitboards
    */
  lazy val guards: Map[Square, BitBoard] = {
    for {
      (s, p) <- board if p.owner == !turn && p.isRanged
      k <- getKing(turn)
      bt = s.getBetweenBB(k) if Attack.getRangedAttack(p, s, BitBoard.empty).get(k)
      g = bt & occupancy if g.count == 1
    } yield {
      g.toList.head -> bt
    }
  }

  /**
    * Check if the player is checked.
    */
  lazy val isChecked: Boolean = turnsKing.exists(getAttackBB(!turn).get)

  def getNonSuicidalMovesOnBoard: Map[Square, BitBoard] = for ((sq, bb) <- attackBBOnBoard(turn)) yield {
    if (board(sq).ptype == KING)
      sq -> (bb & ~getAttackBB(!turn))
    else if (guards.keySet.contains(sq))
      sq -> (bb & guards(sq))
    else
      sq -> bb
  }

  def generateLegalMovesOnBoard(m: Map[Square, BitBoard]): Map[(Square, Piece), BitBoard] = for {
    (s, bb) <- m
    (p, b) <- Attack.getSeq(board(s), s, bb)
  } yield {
    (s, p) -> (b & ~occupancy(turn))
  }

  def getEscapeMoves: Map[(Square, Piece), BitBoard] = {
    require(turnsKing.isDefined)

    // king's move
    val king = turnsKing.get
    val kingEscape = Map((king, Piece(turn, KING)) -> (attackBBOnBoard(turn)(king) & ~(getAttackBB(!turn) | occupancy(turn) | attackerPotentialBB)))

    // move a piece between king and the attacker or capture the attacker (except king's move)
    val attacker = if (attackers.size == 1) attackers.headOption else None
    val between = attacker.map(king.getBetweenBB)
    val betweenAndAttacker = attacker.map(atk => between.get.set(atk))

    val moveBetween = for ((sq, bb) <- getNonSuicidalMovesOnBoard if sq != king; bt <- betweenAndAttacker) yield sq -> (bb & bt)

    // drop a piece between king and the attacker
    val dropBetween = for (((sq, p), bb) <- attackBBInHand; bt <- between) yield (sq, p) -> (bb & bt)

    kingEscape ++ generateLegalMovesOnBoard(moveBetween) ++ dropBetween
  }

  /**
    * All legal moves in the bitboard description
    *
    * @return map of the square from which piece moves, new piece, and attack bitboard
    */
  lazy val legalMovesBB: Map[(Square, Piece), BitBoard] = {
    val m = if (isChecked)
      getEscapeMoves
    else
      generateLegalMovesOnBoard(getNonSuicidalMovesOnBoard) ++ attackBBInHand
    m.filter(_._2.nonEmpty)
  }

  def legalMoves: Seq[ExtendedMove] = (for {
    ((from, p), bb) <- legalMovesBB
    to <- bb.toList
    mv <- ExtendedMove.fromMove(Move(from, to, None, Some(p.ptype), None), this) // todo: improve? State#generateExtendedMove ?
  } yield mv).toSeq

  /**
    * Check if the move is legal.
    *
    * @param move move to test
    * @return true if the move is legal
    */
  def isValidMove(move: ExtendedMove): Boolean = legalMoves.contains(move.copy(elapsedTime = None))

  /** *
    * Check if the state is mated.
    *
    * @return true if mated
    */
  def isMated: Boolean = legalMovesBB.isEmpty

  /**
    * Make one move.
    *
    * @param move move to make
    * @return new state
    */
  def makeMove(move: ExtendedMove): Option[State] = isValidMove(move).option {
    val releaseHand: HandType => HandType = move.isDrop.when(MapUtil.decrementMap(_, move.newPiece))
    val obtainHand: HandType => HandType = move.capturedPiece.when(p => h => MapUtil.incrementMap(h, !p.demoted))
    State(!turn, board - move.from + (move.to -> move.newPiece), (releaseHand andThen obtainHand) (hand))
  }

  def getPieceCount: Map[Piece, Int] = MapUtil.mergeMaps(board.groupBy(_._2).mapValues(_.size), hand)(_ + _, 0)

  def getUsedPtypeCount: Map[Ptype, Int] = getPieceCount.groupBy(_._1.ptype.demoted).mapValues(_.values.sum)

  def getUnusedPtypeCount: Map[Ptype, Int] = MapUtil.mergeMaps(State.capacity, getUsedPtypeCount)(_ - _, 0)

  def checkCapacity: Boolean = getPieceCount.filterKeys(_.ptype == KING).forall(_._2 <= 1) && getUnusedPtypeCount.values.forall(_ >= 0)

  def canAttack(from: Square, to: Square): Boolean = {
    require(from != HAND, "from must not be in hand")
    attackBBOnBoard(turn).get(from).exists(_.get(to))
  }
}

object State extends CsaStateReader with SfenStateReader {

  type BoardType = Map[Square, Piece]
  type HandType = Map[Piece, Int]

  object PromotionFlag extends Enumeration {
    type PromotionFlag = Value
    val CannotPromote, CanPromote, MustPromote = Value
  }

  val EMPTY_HANDS: HandType = (for (t <- Player.constructor; pt <- Ptype.inHand) yield Piece(t, pt) -> 0).toMap

  val empty = State(BLACK, Map.empty, EMPTY_HANDS)
  lazy val capacity: Map[Ptype, Int] = Map(PAWN -> 18, LANCE -> 4, KNIGHT -> 4, SILVER -> 4, GOLD -> 4, BISHOP -> 2, ROOK -> 2, KING -> 2)

  /**
    * Get the square where the turn-to-move player's king.
    *
    * @return None if the king is not on board
    */
  def getKingSquare(player: Player, board: BoardType): Option[Square] =
    board.view.filter { case (s, p) => p == Piece(player, KING) }.map(_._1).headOption

  // constant states
  val HIRATE = State(BLACK, Map(
    Square(1, 1) -> Piece(WHITE, LANCE),
    Square(2, 1) -> Piece(WHITE, KNIGHT),
    Square(3, 1) -> Piece(WHITE, SILVER),
    Square(4, 1) -> Piece(WHITE, GOLD),
    Square(5, 1) -> Piece(WHITE, KING),
    Square(6, 1) -> Piece(WHITE, GOLD),
    Square(7, 1) -> Piece(WHITE, SILVER),
    Square(8, 1) -> Piece(WHITE, KNIGHT),
    Square(9, 1) -> Piece(WHITE, LANCE),
    Square(2, 2) -> Piece(WHITE, BISHOP),
    Square(8, 2) -> Piece(WHITE, ROOK),
    Square(1, 3) -> Piece(WHITE, PAWN),
    Square(2, 3) -> Piece(WHITE, PAWN),
    Square(3, 3) -> Piece(WHITE, PAWN),
    Square(4, 3) -> Piece(WHITE, PAWN),
    Square(5, 3) -> Piece(WHITE, PAWN),
    Square(6, 3) -> Piece(WHITE, PAWN),
    Square(7, 3) -> Piece(WHITE, PAWN),
    Square(8, 3) -> Piece(WHITE, PAWN),
    Square(9, 3) -> Piece(WHITE, PAWN),
    Square(1, 7) -> Piece(BLACK, PAWN),
    Square(2, 7) -> Piece(BLACK, PAWN),
    Square(3, 7) -> Piece(BLACK, PAWN),
    Square(4, 7) -> Piece(BLACK, PAWN),
    Square(5, 7) -> Piece(BLACK, PAWN),
    Square(6, 7) -> Piece(BLACK, PAWN),
    Square(7, 7) -> Piece(BLACK, PAWN),
    Square(8, 7) -> Piece(BLACK, PAWN),
    Square(9, 7) -> Piece(BLACK, PAWN),
    Square(2, 8) -> Piece(BLACK, ROOK),
    Square(8, 8) -> Piece(BLACK, BISHOP),
    Square(1, 9) -> Piece(BLACK, LANCE),
    Square(2, 9) -> Piece(BLACK, KNIGHT),
    Square(3, 9) -> Piece(BLACK, SILVER),
    Square(4, 9) -> Piece(BLACK, GOLD),
    Square(5, 9) -> Piece(BLACK, KING),
    Square(6, 9) -> Piece(BLACK, GOLD),
    Square(7, 9) -> Piece(BLACK, SILVER),
    Square(8, 9) -> Piece(BLACK, KNIGHT),
    Square(9, 9) -> Piece(BLACK, LANCE)
  ), EMPTY_HANDS)

  val MATING_BLACK = State(BLACK, Map(
    Square(5, 1) -> Piece(WHITE, KING)
  ), EMPTY_HANDS ++ Map(
    Piece(BLACK, ROOK) -> 2,
    Piece(BLACK, BISHOP) -> 2,
    Piece(BLACK, GOLD) -> 4,
    Piece(BLACK, SILVER) -> 4,
    Piece(BLACK, KNIGHT) -> 4,
    Piece(BLACK, LANCE) -> 4,
    Piece(BLACK, PAWN) -> 18
  ))

  val MATING_WHITE = State(WHITE, Map(
    Square(5, 9) -> Piece(BLACK, KING)
  ), EMPTY_HANDS ++ Map(
    Piece(WHITE, ROOK) -> 2,
    Piece(WHITE, BISHOP) -> 2,
    Piece(WHITE, GOLD) -> 4,
    Piece(WHITE, SILVER) -> 4,
    Piece(WHITE, KNIGHT) -> 4,
    Piece(WHITE, LANCE) -> 4,
    Piece(WHITE, PAWN) -> 18
  ))
}