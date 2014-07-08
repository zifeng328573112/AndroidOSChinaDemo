package net.oschina.app.bean;

/**
 * 举报实体类
 * @author 火蚁（http://my.oschina.net/LittleDY）
 * @version 1.0
 * @created 2014-02-13
 */
public class Report extends Entity{
	public final static String REPORT_ID = "id";
	public final static String REPORT_LINK = "link";
	public final static String REPORT_REASON = "reason";
	public final static String REPORT_OTHER_REASON = "otherreason";
	private int reportId;
	private String linkAddress;
	private String reason;
	private String otherReason;
	public String getOtherReason() {
		return otherReason;
	}
	public void setOtherReason(String otherReason) {
		this.otherReason = otherReason;
	}
	public int getReportId() {
		return reportId;
	}
	
	public void setReportId(int reportId) {
		this.reportId = reportId;
	}
	public String getLinkAddress() {
		return linkAddress;
	}
	public void setLinkAddress(String linkAddress) {
		this.linkAddress = linkAddress;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	@Override
	public String toString() {
		return "Report [reportId=" + reportId + ", linkAddress=" + linkAddress
				+ ", reason=" + reason + ", otherReason=" + otherReason + "]";
	}
}
