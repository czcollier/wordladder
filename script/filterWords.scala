/**
 * User: ccollier
 * Date: 1/10/13
 * Time: 2:55 PM
 */

val pat = "[a-z]+".r.pattern
val words = scala.io.Source.fromFile(args(0)).getLines().zipWithIndex.toArray
val numSourceWords = words.length
val numTargetWords = Integer.parseInt(args(1))

val filtered = words.filter(w => pat.matcher(w._1).matches)
val filteredSize = filtered.length

val targetRatio = filteredSize / numTargetWords
val cut = filtered.filter(w => w._2 % targetRatio == 0)
val cutSize = cut.length

for ((w, i) <- cut)
    println(w)


println(
  """
initial size: %d
after filter: %d
after cut:    %d
""".format(numSourceWords, filteredSize, cutSize))
