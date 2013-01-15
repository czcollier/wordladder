package net.xorf.wl

import scala.math.min
import java.util

object DistanceMeasures {
  private def minimum(i1: Int, i2: Int, i3: Int) = min(min(i1, i2), i3)

  def levenshtein1(s1:String, s2:String) = {
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) {
      (j, i) => if (j == 0) i else if (i == 0) j else 0
    }

    for(j <- 1 to s2.length; i <- 1 to s1.length)
      dist(j)(i) =
        if (s2(j - 1) == s1(i - 1)) dist(j - 1)(i - 1)
        else minimum(dist(j - 1)(i), dist(j)(i - 1), dist(j - 1)(i - 1)) + 1

    dist(s2.length)(s1.length)
  }

  def levenshtein3(s1:String, s2:String): Int = {
    val dist = Array.tabulate(s2.length + 1, s1.length + 1) {
      (j, i) => if (j == 0) i else if (i == 0) j else 0
    }

    for(j <- 1 to s2.length; i <- 1 to s1.length) {
      val d =
        if (s2(j - 1) == s1(i - 1)) dist(j - 1)(i - 1)
        else minimum(dist(j - 1)(i), dist(j)(i - 1), dist(j - 1)(i - 1)) + 1

      if (d > 1) return 100 else dist(j)(i) = d
    }
    dist(s2.length)(s1.length)
  }

  val dist2 = Array.ofDim[Int](128, 128)

  def resetDist(dist: Array[Array[Int]]) {
    for (j <- 0 until 128; i <- 0 until 128) {
      dist2(j)(i) = (if (j == 0) i else if (i == 0) j else 0)
    }
  }

  def levenshtein2(s1:String, s2:String): Int = {
    _levenshtein2(dist2, s1, s2)
  }

  def _levenshtein2(dist: Array[Array[Int]], s1:String, s2:String): Int = {
    resetDist(dist)
    for(j <- 1 to s2.length; i <- 1 to s1.length) {
      val d =
        if (s2(j - 1) == s1(i - 1)) dist(j - 1)(i - 1)
        else minimum(dist(j - 1)(i), dist(j)(i - 1), dist(j - 1)(i - 1)) + 1

      if (d > 1) return 100 else dist(j)(i) = d
    }
    dist(s2.length)(s1.length)
  }
}
