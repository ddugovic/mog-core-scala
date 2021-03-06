package com.mogproject.mogami.core.attack

import scala.annotation.tailrec
import com.mogproject.mogami._
import com.mogproject.mogami.util.Implicits._

/**
  *
  */
trait RangedAttack extends DirectAttack {
  def getRangedAttack(piece: Piece, square: Square, occ: BitBoard): BitBoard = {
    require(piece.isRanged, s"Invalid piece: ${piece}")

    @tailrec
    def f(sofar: BitBoard, offsets: Seq[(Int, Int)]): BitBoard = offsets match {
      case (fileOffset, rankOffset) :: xs =>
        val md = math.min(getMaxDistance(square.file, fileOffset), getMaxDistance(square.rank, rankOffset))
        f(g(sofar, 1, md, fileOffset, rankOffset), xs)
      case Nil => sofar
    }

    @tailrec
    def g(sofar: BitBoard, distance: Int, maxDistance: Int, fileOffset: Int, rankOffset: Int): BitBoard = {
      if (distance > maxDistance) {
        sofar
      } else {
        val s = Square(square.file + fileOffset * distance, square.rank + rankOffset * distance)
        val bb = sofar.set(s)

        if (distance == maxDistance || occ.get(s))
          bb
        else
          g(sofar.set(s), distance + 1, maxDistance, fileOffset, rankOffset)
      }
    }

    val offsets = piece.ptype.demoted match {
      case LANCE => Seq((0, piece.owner.isBlack.fold(-1, 1)))
      case BISHOP => Seq((-1, -1), (-1, 1), (1, -1), (1, 1))
      case ROOK => Seq((-1, 0), (0, -1), (0, 1), (1, 0))
      // $COVERAGE-OFF$
      case _ => throw new RuntimeException("Never happens.")
      // $COVERAGE-ON$
    }

    f(BitBoard.empty, offsets) | piece.isPromoted.fold(getDirectAttack(Piece(BLACK, KING), square), BitBoard.empty)
  }

  protected[attack] def getMaxDistance(base: Int, step: Int): Int = (step == 0).fold(9, (5 + 4 * step - base) / step)

}
