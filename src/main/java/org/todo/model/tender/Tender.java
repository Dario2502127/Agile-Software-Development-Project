package org.todo.model.tender;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Tender {
	public Long id;
	public String name;
	public LocalDate noticeDate;
	public LocalDate closeDate;
	public LocalDate discloseDate; // nullable
	public String status;          // Open / Closed / Awarded
	public String staffEmail;      // creator/owner
	public String description;
	public String termOfConstruction;
	public BigDecimal estimatedPrice;
	public String winnerReason;    // nullable
	public Long winnerBidId;       // nullable

	// >>> NEW: single business category used for search/eligibility <<<
	public String category;        // e.g. "Computer System"
}
