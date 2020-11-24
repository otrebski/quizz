package tree.db

import java.util.Date

case class Feedback(
    id: Int,
    timestamp: Date,
    treeId: String,
    path: String,
    comment: String,
    rate: Int
)
