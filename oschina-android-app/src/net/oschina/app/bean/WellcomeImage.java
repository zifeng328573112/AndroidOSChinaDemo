package net.oschina.app.bean;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import net.oschina.app.AppException;
import net.oschina.app.common.StringUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.tencent.mm.sdk.platformtools.Log;

import android.util.Xml;

/**
 * 欢迎界面实体类
 * @author 火蚁 (http://my.oschina.net/LittleDY)
 * @version 1.0
 * @created 2014-02-24
 */
@SuppressWarnings("serial")
public class WellcomeImage implements Serializable{
	
	public final static String UTF8 = "UTF-8";
	public final static String NODE_ROOT = "oschina";
	
	private boolean update;
	private String startDate;
	private String endDate;
	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	private String downloadUrl;
	
	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public static WellcomeImage parse(InputStream inputStream) throws IOException, AppException {
		WellcomeImage update = null;
        XmlPullParser xmlParser = Xml.newPullParser();
        try {        	
            xmlParser.setInput(inputStream, UTF8);
            int evtType=xmlParser.getEventType();
			while(evtType!=XmlPullParser.END_DOCUMENT){ 
	    		String tag = xmlParser.getName(); 
			    switch(evtType){ 
			    	case XmlPullParser.START_TAG:			    		
			            if(tag.equalsIgnoreCase("android"))
			    		{
			            	update = new WellcomeImage();
			    		}
			            else if(update != null)
			    		{
			    			if(tag.equalsIgnoreCase("coverUpdate"))
				            {			      
			    				update.setUpdate(Boolean.parseBoolean(xmlParser.nextText()));
				            }
				            else if(tag.equalsIgnoreCase("coverStartDate"))
				            {			            	
				            	update.setStartDate(xmlParser.nextText());
				            }
				            else if(tag.equalsIgnoreCase("coverEndDate"))
				            {			            	
				            	update.setEndDate(xmlParser.nextText());
				            }
				            else if(tag.equalsIgnoreCase("coverURL"))
				            {			            	
				            	update.setDownloadUrl(xmlParser.nextText());
				            }
			    		}
			    		break;
			    	case XmlPullParser.END_TAG:		    		
				       	break; 
			    }
			    evtType=xmlParser.next();
			}		
        } catch (XmlPullParserException e) {
			throw AppException.xml(e);
        } finally {
        	inputStream.close();	
        }      
        return update;       
	}

	@Override
	public String toString() {
		return "WellcomeImage [update=" + update + ", startDate=" + startDate
				+ ", endDate=" + endDate + ", downloadUrl=" + downloadUrl + "]";
	}
	
}
