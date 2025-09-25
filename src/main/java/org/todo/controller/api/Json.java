package org.todo.controller.api;

import org.todo.model.tender.Bid;
import org.todo.model.tender.Tender;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

final class Json {
	private Json(){}

	static String esc(String s){
		if (s==null) return "null";
		return "\"" + s.replace("\\","\\\\").replace("\"","\\\"") + "\"";
	}

	static String tender(Tender t){
		return "{"
				+ "\"id\":"+t.id
				+ ",\"name\":"+esc(t.name)
				+ ",\"notice_date\":"+esc(t.noticeDate.toString())
				+ ",\"close_date\":"+esc(t.closeDate.toString())
				+ ",\"disclose_date\":"+(t.discloseDate==null?"null":esc(t.discloseDate.toString()))
				+ ",\"status\":"+esc(t.status)
				+ ",\"description\":"+esc(t.description)
				+ ",\"term\":"+esc(t.termOfConstruction)
				+ ",\"estimated_price\":"+(t.estimatedPrice==null?"null":t.estimatedPrice.toPlainString())
				+ ",\"winner_reason\":"+esc(t.winnerReason)
				+ ",\"winner_bid_id\":"+(t.winnerBidId==null?"null":t.winnerBidId)
				+ "}";
	}

	static String tenders(List<Tender> list){
		return "[" + list.stream().map(Json::tender).collect(Collectors.joining(",")) + "]";
	}

	static String bid(Bid b){
		return "{"
				+ "\"id\":"+b.id
				+ ",\"tender_id\":"+b.tenderId
				+ ",\"company_id\":"+esc(b.companyId)
				+ ",\"company_name\":"+esc(b.companyName)
				+ ",\"bid_price\":"+b.bidPrice.toPlainString()
				+ ",\"created_at\":"+esc(b.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
				+ "}";
	}

	static String bids(List<Bid> list){
		return "[" + list.stream().map(Json::bid).collect(Collectors.joining(",")) + "]";
	}
}
