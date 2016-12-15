package com.mogproject.mogami.core.attack

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{FlatSpec, MustMatchers}

import com.mogproject.mogami._
import com.mogproject.mogami.core.PieceConstant._
import com.mogproject.mogami.core.SquareConstant._

class AttackSpec extends FlatSpec with MustMatchers with GeneratorDrivenPropertyChecks {
  "Attack#get" must "work with aerial attacks" in {
    val allOcc = BitBoard.seq(
      """
        |********* ********* *-------- --------- *-------- --------- -------*- *-------*
        |-*-----*- --------- -*------- --------- -*-----*- --------- ----**--- ----*----
        |--------- --*------ ********* --------- --*------ ------*-- ----**--- ---*-----
        |--------- ------*-- ---*----- --------- ---**---- --------- --------- -----*---
        |--------- -----*--- ----*---- ----*---- ----**--- ---*----- --**----- ---------
        |------*-- --*------ -----*--- --------- ----*-*-- *-*-**--- --------* --*---*--
        |--------- -----*--- ------*-- --------- -------*- -*------- *-------- ---***---
        |-*-----*- --------- -------*- ********* --------* --------- --------- ---***---
        |********* --------- --------* --------- --------- --------- ----*--*- ---------
      """.stripMargin
    )
    val myOcc = BitBoard.seq(
      """
        |********* ********* --------- --------- *-------- --------- -------*- *-------*
        |-*-----*- --------- --------- --------- -*------- --------- --------- ----*----
        |--------- --------- ********* --------- --*------ --------- --------- ---*-----
        |--------- --------- --------- --------- ---**---- --------- --------- -----*---
        |--------- --------- --------- --------- ----**--- ---*----- --**----- ---------
        |------*-- --------- --------- --------- ----*-*-- *-*-**--- --------* --*---*--
        |--------- --------- --------- --------- -------*- -*------- *-------- ---------
        |-*-----*- --------- --------- ********* --------* --------- --------- ---------
        |********* --------- --------- --------- --------- --------- ----*--*- ---------
      """.stripMargin
    )
    val pawnOcc = BitBoard.seq(
      """
        |--------- ********* --------- --------- *-------- --------- --------- *-------*
        |--------- --------- --------- --------- -*------- --------- --------- ----*----
        |--------- --------- ********* --------- --*------ --------- --------- ---------
        |--------- --------- --------- --------- ---*----- --------- --------- ---------
        |--------- --------- --------- --------- -----*--- --------- --**----- ---------
        |--------- --------- --------- --------- ------*-- *---**--- --------* --*---*--
        |--------- --------- --------- --------- -------*- -*------- *-------- ---------
        |--------- --------- --------- ********* --------* --------- --------- ---------
        |--------- --------- --------- --------- --------- --------- --------- ---------
      """.stripMargin
    )
    val expected = BitBoard.seq(
      """
        |--------- --------- --------- --------- --------- --------- --------- ---------
        |*-*****-* --------- --------- --------- ----*---- --**--*** -*----**- -*-*-*-*-
        |********* --------- --------- --------- ----*---- --**---** -*----**- -*---*-*-
        |********* --------- --------- --------- --------- --**--*** -*--****- -*-*---*-
        |********* --------- --------- --------- --------- --*---*** -*--****- -*-*-*-*-
        |******-** --------- --------- --------- --------- ---*--*** -*--****- -*-*-*-*-
        |********* --------- --------- --------- ----*---- --**--*** -*--****- -*-----*-
        |*-*****-* --------- --------- --------- ----*---- --**--*** -*--****- -*-----*-
        |--------- --------- --------- --------- ----*---- --**--*** -*---**-- -*-*-*-*-
      """.stripMargin)

    val occs = Seq(allOcc, myOcc, pawnOcc).transpose
    occs.length must be(expected.length)
    occs zip expected foreach {
      case (aa :: b :: c :: Nil, x) => (aa, Attack.get(BP, HAND, aa, b, c)) must be((aa, x))
      case _ =>
    }
  }
  it must "work with direct attacks" in {
    val poses = Seq(P55, P11, P27, P99, P33, P44, P83, P94, P39, P51)
    val pieces = Seq(BP, WP, WN, BS, WG, BK, WPP, BPL, BS, WK)
    val allOcc = BitBoard.seq(
      """
        |********* ********* *-------- --------- *------*- --------- -------*- *-------* ********* *********
        |-*-----*- --------- -*------- --------- -*----**- --------- ***-**--- ----*---- -*-----*- -*-----*-
        |----*---- --*------ ********* --------- --*--*--- ------*-- ***-**--- -*-*----- ********* *********
        |--------- ------*-- ---*----- --------- ---**---- --------- ***------ -*---*--- --------- ---------
        |--------- -----*--- ----*---- ----*---- ----**--- ---*----- --**----- --------- --------- ---------
        |------*-- --*------ -----*--- --------- ----*-*-- *-*-**--- --------* --*---*-- --------- ---------
        |--------- -----*--- ------*-- --------- -------*- -*------- *-------- ---***--- ********* *********
        |-*-----*- --------- -------*- ********* --------* --------- --------- ---***--- -*-----*- -*-----*-
        |********* --------- --------* --------- --------- --------- ----*--*- --------- ********* *********
      """.stripMargin
    )
    val myOcc = BitBoard.seq(
      """
        |********* ********* --------- --------- *------*- --------- -------*- *-------* --------- *********
        |-*-----*- --------- --------- --------- -*----*-- --------- *-*------ ----*---- --------- -*-----*-
        |--------- --------- ********* --------- --*--*--- ------*-- -*------- -*-*----- --------- *********
        |--------- --------- --------- --------- ---**---- --------- --------- -*---*--- --------- ---------
        |--------- --------- --------- --------- ----**--- ---*----- --**----- --------- --------- ---------
        |------*-- --------- --------- --------- ----*-*-- *-*-**--- --------* --*---*-- --------- ---------
        |--------- --------- --------- --------- -------*- -*------- *-------- --------- ********* ---------
        |-*-----*- --------- --------- *-******* --------* --------- --------- --------- -*-----*- ---------
        |********* --------- --------- --------- --------- --------- ----*--*- --------- ********* ---------
      """.stripMargin
    )
    val pawnOcc = BitBoard.seq(
      """
        |--------- ********* --------- --------- *-------- --------- --------- *-------* --------- ---------
        |--------- --------- --------- --------- -*------- --------- --------- ----*---- --------- ---------
        |--------- --------- ********* --------- --*------ --------- --------- --------- --------- *********
        |--------- --------- --------- --------- ---*----- --------- --------- --------- --------- ---------
        |--------- --------- --------- --------- -----*--- --------- --**----- --------- --------- ---------
        |--------- --------- --------- --------- ------*-- *---**--- --------* --*---*-- --------- ---------
        |--------- --------- --------- --------- -------*- -*------- *-------- --------- ********* ---------
        |--------- --------- --------- ********* --------* --------- --------- --------- --------- ---------
        |--------- --------- --------- --------- --------- --------- --------- --------- --------- ---------
      """.stripMargin
    )
    val expected = BitBoard.seq(
      """
        |--------- --------- --------- --------- --------- --------- --------- --------- --------- ---------
        |--------- --------* --------- --------- --------- --------- -*------- --------- --------- ---***---
        |--------- --------- --------- --------- -------*- ----**--- *-*------ *-------- --------- ---------
        |----*---- --------- --------- --------- -----***- ----*-*-- ***------ --------- --------- ---------
        |--------- --------- --------- --------- --------- ----***-- --------- *-------- --------- ---------
        |--------- --------- --------- --------- --------- --------- --------- --------- --------- ---------
        |--------- --------- --------- --------- --------- --------- --------- --------- --------- ---------
        |--------- --------- --------- -*------- --------- --------- --------- --------- -----**-- ---------
        |--------- --------- ------*-* --------- --------- --------- --------- --------- --------- ---------
      """.stripMargin)

    val params = Seq(poses, pieces, allOcc, myOcc, pawnOcc).transpose
    params.length must be(expected.length)
    params zip expected foreach {
      case (pos :: piece :: aa :: b :: c :: Nil, x) =>
        (aa, Attack.get(piece.asInstanceOf[Piece], pos.asInstanceOf[Square], aa.asInstanceOf[BitBoard],
          b.asInstanceOf[BitBoard], c.asInstanceOf[BitBoard])) must be((aa, x))
      case _ =>
    }
  }
  it must "work with ranged attacks" in {
    val poses = Seq(P55, P11, P27, P99, P33, P44, P83, P94, P19, P82)
    val pieces = Seq(BL, WL, BB, WB, BR, WR, BPB, WPR, BL, WR)
    val allOcc = BitBoard.seq(
      """
        |********* ********* *-------- --------- *------*- --------- -------*- *-------* ********* *********
        |-*-----*- --------- -*------- --------- -*----**- --------- ***-**--- ----*---- -*-----*- -*-----*-
        |----*---- --*------ ********* --------- --*--*--- ------*-- ***-**--- -*-*----- ********* *********
        |--------- ------*-- ---*----- --------- ---**---- --------* **------- -*---*--- --------- ---------
        |--------- -----*--- ----*---- ----*---- ----**--- ---*----- --**----- --------- --------- ---------
        |------*-- --*------ -----*--- --------- ----*-*-- *-*-**--- --------* --*---*-- --------- ---------
        |--------- -----*--- ------*-- --------- -------*- -*------- *-------- ---***--- ********* *********
        |-*-----*- --------- -------*- *-******* --------* --------- --------- ---***--- -*-----*- -*-----*-
        |********* --------- --------* --------- --------- --------- ----*--*- --------- ********* *********
      """.stripMargin
    )
    val myOcc = BitBoard.seq(
      """
        |********* ********* --------- --------- *------*- --------- -------*- *-------* --------- *********
        |-*-----*- --------- --------- --------- -*----*-- --------- *-*------ ----*---- --------- -*-----*-
        |--------- --------- ********* --------- --*--*--- ------*-- -*------- -*-*----- --------- *********
        |--------- --------- --------- --------- ---**---- --------* --------- -*---*--- --------- ---------
        |--------- --------- --------- --------- ----**--- ---*----- --*------ --------- --------- ---------
        |------*-- --------- --------- --------- ----*-*-- *-*-*---- --------* --*---*-- --------- ---------
        |--------- --------- --------- --------- -------*- -*------- *-------- --------- ********* ---------
        |-*-----*- --------- --------- *-******* --------* --------- --------- --------- -*-----*- ---------
        |********* --------- --------- --------- --------- --------- ----*--*- --------- ********* ---------
      """.stripMargin
    )
    val pawnOcc = BitBoard.seq(
      """
        |--------- ********* --------- --------- *-------- --------- --------- *-------* --------- ---------
        |--------- --------- --------- --------- -*------- --------- --------- ----*---- --------- ---------
        |--------- --------- ********* --------- --*------ --------- --------- --------- --------- *********
        |--------- --------- --------- --------- ---*----- --------- --------- --------- --------- ---------
        |--------- --------- --------- --------- -----*--- --------- --**----- --------- --------- ---------
        |--------- --------- --------- --------- ------*-- *---*---- --------* --*---*-- --------- ---------
        |--------- --------- --------- --------- -------*- -*------- *-------- --------- ********* ---------
        |--------- --------- --------- *-******* --------* --------- --------- --------- --------- ---------
        |--------- --------- --------- --------- --------- --------- --------- --------- --------- ---------
      """.stripMargin
    )
    val expected = BitBoard.seq(
      """
        |--------- --------- --------- --------- --------- -----*--- --------- --------- --------- ---------
        |--------- --------* --------- --------- --------- -----*--- -*------- *-------- --------- *-*****--
        |----*---- --------* --------- --------- -------** -----*--- *-*------ *-------- --------- ---------
        |----*---- --------* ----*---- --------- ------*-- *****-**- ***------ --------- --------- ---------
        |--------- --------* -----*--- ----*---- ------*-- -----*--- ---*----- **------- --------- ---------
        |--------- --------* ------*-* ---*----- --------- -----*--- --------- *-------- --------- ---------
        |--------- --------* --------- --*------ --------- --------- --------- *-------- --------- ---------
        |--------- --------* ------*-* -*------- --------- --------- --------- *-------- --------* ---------
        |--------- --------* -----*--- --------- --------- --------- --------- *-------- --------- ---------
      """.stripMargin)

    val params = Seq(poses, pieces, allOcc, myOcc, pawnOcc).transpose
    params.length must be(expected.length)
    params zip expected foreach {
      case (pos :: piece :: aa :: b :: c :: Nil, x) =>
        (aa, Attack.get(piece.asInstanceOf[Piece], pos.asInstanceOf[Square], aa.asInstanceOf[BitBoard],
          b.asInstanceOf[BitBoard], c.asInstanceOf[BitBoard])) must be((aa, x))
      case _ =>
    }
  }

  "Attack#getSeq" must "return sequence of attack bitboards" in {
    Attack.getSeq(BP, P12, BitBoard("001.000.000.000.000.000.000.000.000")) mustBe Seq(
      (BPP, BitBoard("001.000.000.000.000.000.000.000.000"))
    )
    Attack.getSeq(BP, P13, BitBoard("000.001.000.000.000.000.000.000.000")) mustBe Seq(
      (BP, BitBoard("000.001.000.000.000.000.000.000.000")),
      (BPP, BitBoard("000.001.000.000.000.000.000.000.000"))
    )
    Attack.getSeq(BP, P15, BitBoard("000.000.000.001.000.000.000.000.000")) mustBe Seq(
      (BP, BitBoard("000.000.000.001.000.000.000.000.000"))
    )
    Attack.getSeq(WL, P57, BitBoard("000.000.000.000.000.000.000.020.020")) mustBe Seq(
      (WL, BitBoard("000.000.000.000.000.000.000.020.000")),
      (WPL, BitBoard("000.000.000.000.000.000.000.020.020"))
    )
    Attack.getSeq(BN, P44, BitBoard("000.012.000.000.000.000.000.000.000")) mustBe Seq(
      (BPN, BitBoard("000.012.000.000.000.000.000.000.000"))
    )
    Attack.getSeq(BPN, P44, BitBoard("000.000.016.012.004.000.000.000.000")) mustBe Seq(
      (BPN, BitBoard("000.000.016.012.004.000.000.000.000"))
    )
    Attack.getSeq(BG, P44, BitBoard("000.000.016.012.004.000.000.000.000")) mustBe Seq(
      (BG, BitBoard("000.000.016.012.004.000.000.000.000"))
    )
    Attack.getSeq(WB, P56, BitBoard("000.401.202.104.050.000.050.104.401")) mustBe Seq(
      (WB, BitBoard("000.401.202.104.050.000.050.104.401")),
      (WPB, BitBoard("000.000.000.000.000.000.050.104.401"))
    )
    Attack.getSeq(WPB, P56, BitBoard("000.401.202.104.070.050.070.104.401")) mustBe Seq(
      (WPB, BitBoard("000.401.202.104.070.050.070.104.401"))
    )
    Attack.getSeq(WB, P57, BitBoard("000.000.401.202.104.050.000.050.104")) mustBe Seq(
      (WB, BitBoard("000.000.401.202.104.050.000.050.104")),
      (WPB, BitBoard("000.000.401.202.104.050.000.050.104"))
    )
  }
}
