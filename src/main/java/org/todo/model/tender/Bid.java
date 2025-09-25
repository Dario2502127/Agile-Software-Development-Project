package org.todo.model.tender;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid {
	public Long id;
	public Long tenderId;
	public String companyId;
	public String companyName;
	public BigDecimal bidPrice;
	public LocalDateTime createdAt;
}
