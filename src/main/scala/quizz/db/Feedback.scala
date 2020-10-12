package quizz.db

import java.util.Date

case class Feedback(
    id: Int,
    timestamp: Date,
    quizzId: String,
    path: String,
    comment: String,
    rate: Int
)
