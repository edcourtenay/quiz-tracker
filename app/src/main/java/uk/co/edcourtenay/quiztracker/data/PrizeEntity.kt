package uk.co.edcourtenay.quiztracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A prize awarded to a top-scoring team.
 *
 * Prizes are configured by the user (add / rename / revalue / reorder / remove),
 * so the number of prizes and their values are not hard-coded. [value] is
 * nullable so a prize can be non-cash ("Bottle of wine").
 */
@Entity(tableName = "prizes")
data class PrizeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val value: Double? = null,
    /** Award order; position 0 is first prize. */
    val position: Int = 0,
)
