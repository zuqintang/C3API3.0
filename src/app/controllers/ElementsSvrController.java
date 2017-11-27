package app.controllers;

import app.models.Element;
import app.models.VOption;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;
import org.javalite.http.Get;
import org.javalite.http.Http;

import sun.misc.Version;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementsSvrController extends BasicController {
    @POST
    public void searchOptions() {
        int pageSize = 5;
        int offset = 0;
        String kw = "";
        long total = 0;
        ArrayList data = new ArrayList();
        if (null != param("limit"))
            pageSize = Integer.parseInt(param("limit"));
        if (null != param("offset"))
            offset = Integer.parseInt(param("offset"));
        if (null != param("keyword"))
            kw = param("keyword");
        String query = "FIELDCODE_VALUE_CN_NAME like ? OR FIELDCODE_TABLECODE = ?";
        Object[] param = new Object[2];
        param[0] = "%" + kw + "%";
        param[1] = kw;
        total = VOption.count(query, param);
        LazyList voptionList = VOption.where(query, param)
                .limit(pageSize).offset(offset).orderBy("ID ASC");
        List<Map> nodes = voptionList.toMaps();
        for (Map node : nodes) {
            Map newNode = new HashMap();
            newNode.put("tabCode", node.get("FIELDCODE_TABLECODE"));
            newNode.put("name", node.get("FIELDCODE_VALUE_CN_NAME"));
            newNode.put("avalue", node.get("FIELDCODE_VALUE"));
            newNode.put("cvalue", node.get("FIELD_COMPUTE"));
            newNode.put("sort", node.get("FIELDORDER"));
            LazyList elementList = Element.where("FIELDCODE_TABLECODE = ?", node.get("FIELDCODE_TABLECODE"));
            if (elementList.size() > 0) {
                List<Map> elements = elementList.toMaps();
                newNode.put("groupName", elements.get(0).get("METADATAFIELD_NAME"));
            } else {
                newNode.put("groupName", "无");
            }
            data.add(newNode);
        }
        Map<String, Object> res = new HashMap<String, Object>();
        res.put("total", total);
        res.put("rows", data);
        toJSON(res);
    }

    @POST
    public void model() {
        if (null == param("id")) {
            output(20001, "参数异常");
            return;
        }
        LazyList eleList = Element.where("ID = ?", param("id"));
        List<Map> elements = eleList.toMaps();
        Map[] nodes = new Map[elements.size()];
        int i = 0;
        for (Map element : elements) {
            nodes[i] = element;
            if (null != element.get("FIELDCODE_TABLECODE")) {
                LazyList vopList = VOption.where("FIELDCODE_TABLECODE = ?", element.get("FIELDCODE_TABLECODE"));
                List<Map> voptions = vopList.toMaps();
                nodes[i].put("voptions", voptions);
                nodes[i].put("FIELD_FROM", voptions.get(0).get("FIELD_FROM"));
            }
            ++i;
        }
        output(0, nodes);
    }

    @POST
    public void save() {
        if (null == param("elementName[i]")) {
            output(20001, "参数异常");
            return;
        }
        LazyList eleList = Element.where("METADATA_NAME = ?", param("elementName[i]"));
        if (eleList.size() > 0) {
            output(20001, "已有该数据元");
            return;
        }
        Map element = new HashMap();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String VERSION_DATE = format.format(System.currentTimeMillis());
        element.put("METADATA_NAME", param("elementName[i]"));
        element.put("CREATE_MAN", "张琪");
        element.put("DATA_UNIT", param("dataUnit"));
        element.put("DATA_META_DATATYPE", param("dataType"));
        element.put("METADATAFIELD_NAME", param("fieldName"));
        element.put("AUDIT", 1);
        element.put("STATUS", 0);
        element.put("DATAMETA_FROM", "CDE");
        element.put("CREATE_DATE", VERSION_DATE);
        element.put("VERSION_DATE", VERSION_DATE);
        Element e = new Element();
        e.fromMap(element);
        if (e.save()) {
            long insertID = Integer.parseInt(Element.find("METADATA_NAME = ?", e.get("METADATA_NAME")).get(0).get("ID").toString());
            element.put("ID", insertID);
            element.put("METADATA_IDENTIFY", element.get("ID"));
            element.put("METADATA_INNER_IDENTIFY", element.get("ID"));
            if (null != param("voptions") && !param("voptions").isEmpty()) {
                Map[] voptions = JsonHelper.toMaps(param("voptions"));
                for (Map voption : voptions) {
                    Map data = new HashMap();
                    data.put("FIELDCODE_VALUE_CN_NAME", voption.get("alias"));
                    data.put("FIELDCODE_VALUE", voption.get("avalue"));
                    data.put("FIELD_COMPUTE", voption.get("cvalue"));
                    data.put("FIELDORDER", voption.get("sorter"));
                    data.put("METADATAFIELD_NAME", voption.get("fname"));
                    data.put("AUDIT", 1);
                    data.put("FIELDCODE_TABLECODE", element.get("ID"));
                    data.put("FIELD_FROM", "CDE");
                    data.put("CREATE_DATE", VERSION_DATE);
                    data.put("VERSION_DATA", VERSION_DATE);
                    VOption v = new VOption();
                    v.fromMap(data);
                    if (v.save()) {
                        long insertVID = Integer.parseInt(VOption.find("FIELDCODE_TABLECODE = ? AND FIELDCODE_VALUE_CN_NAME=?", e.get("ID"), v.get("FIELDCODE_VALUE_CN_NAME")).get(0).get("ID").toString());
                        data.put("ID", insertVID);
                        element.put("FIELDCODE_TABLECODE", data.get("ID"));
                    } else {
                        output(20001, "值域保存失败");
                    }
                }
            }
            if (null != param("optionCode") && !param("optionCode").isEmpty()) {
                element.put("FIELDCODE_TABLECODE", param("optionCode"));
            }
            e.fromMap(element);
            e.save();
            output(0, "保存成功");
        } else {
            output(20001, "数据元保存失败");
        }
    }

    @POST
    public void getOptions() {
        LazyList<Model> vopList = VOption.where("FIELDCODE_TABLECODE = ?", param("code"))
                .orderBy("FIELDORDER ASC");
        List data = vopList.toMaps();
        output(0, data);
    }

    public void auditContent() {
        LazyList<Model> eleList = Element.where("AUDIT = ?", 0).orderBy("ID ASC");
        List<Map<String, Object>> elements = eleList.toMaps();
        ArrayList data = new ArrayList();
        for (Map element : elements) {
            Map node = new HashMap();
            node.put("code", element.get("METADATA_IDENTIFY"));
            node.put("name", element.get("METADATA_NAME"));
            node.put("unit", element.get("DATA_UNIT"));
            node.put("tableCode", element.get("FiELDCODE_TABLECODE"));
            data.add(node);
        }
        output(0, data);
    }

    @POST
    public void optionsSave() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String VERSION_DATE = format.format(System.currentTimeMillis());
        if (null != param("voptions") && !param("voptions").isEmpty()) {
            Map[] voptions = JsonHelper.toMaps(param("voptions"));
            for (Map voption : voptions)
            {
                Map data = new HashMap();
                data.put("FIELDCODE_VALUE_CN_NAME", voption.get("FIELDCODE_VALUE_CN_NAME"));
                data.put("FIELDCODE_VALUE", voption.get("FIELDCODE_VALUE"));
                data.put("FIELD_COMPUTE", voption.get("FIELD_COMPUTE"));
                data.put("FIELDORDER", voption.get("FIELDORDER"));
                data.put("METADATAFIELD_NAME", voption.get("METADATAFIELD_NAME"));
                data.put("AUDIT", 1);
                data.put("FIELD_FROM", "CDE");
                data.put("CREATE_DATE", VERSION_DATE);
                data.put("VERSION_DATA", VERSION_DATE);
                if(null != voption.get("ID")&&voption.get("ID")!="")
                {
                    data.put("ID",voption.get("ID"));
                }
                VOption v = new VOption();
                v.fromMap(data);
                if (v.save())
                {
                    output(0,data);
                }
                else
                {
                    output(20001, "值域保存失败");
                }
            }
        }
    }

    @POST
    public void dataMatch() {
    	String[] elementNames =param("nameArr").split(",");
    	int limit = Integer.parseInt(param("limit"));
    	int offset = Integer.parseInt(param("offset"));
    	int total = elementNames.length;
    	ArrayList rows = new ArrayList();
    	for(int i = offset; i < offset+limit; i++){
        	String subquery = "DATA_OBJECT_NAME = ? or DATA_DISPLAY = ? or DATA_FEATURE_NAME = ?";
        	String[] params = elementNames[i].split("\\ ",3);
        	for(String param : params){
        		System.out.println(param);
        	}
        	long matchNum = Element.count(subquery,params);
        	ArrayList matchData = new ArrayList();
        	if(matchNum > 0){
        		LazyList<Model> elementList = Element.where(subquery, params);
        		List<Map<String,Object>> elements = elementList.toMaps();
            	for(Map element : elements)
            	{
            		Map node = new HashMap();
            		node.put("METADATA_NAME" , element.get("METADATA_NAME"));
            		node.put("DATA_OBJECT_NAME", element.get("DATA_OBJECT_NAME"));
            		node.put("DATA_DISPLAY",element.get("DATA_DISPLAY"));
            		node.put("DATA_FEATURE_NAME",element.get("DATA_FEATURE_NAME"));
            		matchData.add(node);
            	}
        	}
        	Map row = new HashMap();
        	row.put("name", elementNames[i]);
        	row.put("matchNum", matchNum);
        	row.put("data",matchData);
        	rows.add(row);
    	}
    	Map res = new HashMap();
    	res.put("total", total);
    	res.put("rows", rows);
    	output(0,res);
    }

    @POST
    public void newDataMath()throws Exception{
    	String[] elementNames =param("nameArr").split(",");
    	int limit = Integer.parseInt(param("limit"))>100?100:Integer.parseInt(param("limit"));
    	int offset = Integer.parseInt(param("offset"));
    	int total = elementNames.length;
    	int more = (offset+limit)>elementNames.length?elementNames.length:offset+limit;
    	ArrayList rows = new ArrayList();
    	for(int i = offset; i < more; i++){
    		Map node = new HashMap();
    		List<Map> words = wordSegment(elementNames[i]);

    		ArrayList<String> highlightWords=departWord(words);
    
    		LazyList<Model> elementList =searchElements(highlightWords);
      
    		List<Map<String, Object>> elements = elementList.toMaps();
    		List<Map<String,Object>> data = new ArrayList();
    		for(Map<String, Object> element:elements)
    		{
    			Map newNode = new HashMap();
    			for(String highlightWord:highlightWords)
    			{
    				String elementStr=element.get("METADATA_NAME").toString();
    				if(elementStr.indexOf(highlightWord)!= -1)
    				{
    					int elementLength = elementStr.length();
    					DecimalFormat df = new DecimalFormat("0.00000");
    					if(null == newNode.get("count"))
    					{
    						if(isEnglish(highlightWord))
    							newNode.put("count",3);
    						else
    							newNode.put("count", 1);
    						newNode.put("matchDegree", df.format((float)1/elementLength));
    						newNode.put("METADATA_NAME", element.get("METADATA_NAME"));
    						newNode.put("METADATA_IDENTIFY", element.get("METADATA_IDENTIFY"));
    						data.add(newNode);
    					}
    					else
    					{
    						int newCount = 0;
    						if(isEnglish(highlightWord))
    							newCount =Integer.parseInt(newNode.get("count").toString())+3;
    						else
    							newCount =Integer.parseInt(newNode.get("count").toString())+1;
    						newNode.put("count",newCount);
    						newNode.put("matchDegree",df.format((float)newCount/elementLength));
    					}
    				}
    			}
    		}
    		if (data.size() > 0){
    			data= sortByParam(data);
    			int toIndex = data.size()>=20?20:data.size();
    			node.put("name",elementNames[i]);
    			node.put("matchNum", data.size());
    			node.put("data",data.subList(0, toIndex));
    			rows.add(node);
    		}
    	}
    	Map res = new HashMap();
    	res.put("total", total);
    	res.put("rows", rows);
    	toJSON(res);
    }

    private List<Map> wordSegment(String elementName) throws UnsupportedEncodingException 
	{
		  
		char[] c = elementName.toCharArray();
	  
		List<Map> words = new ArrayList();	  
		//operate Chinese word
		for(int i=0;i<c.length;i++)
		{			
			if(isChinese(c[i])){
				System.out.println(c[i]);
				Map word = new HashMap();
				word.put("cont", c[i]);
				word.put("pos", "n");
				words.add(word);
			}
		}		
		//operate English word
		String englishWord = "";
		for(int i=0;i<c.length;i++)
		{
			//if isEnglish
			if(isEnglish(String.valueOf(c[i]))) 
			{
				//set englishWord = englishWord + c[i];
				englishWord = englishWord+String.valueOf(c[i]);
			}
			//else is not English
			else
			{
				if(englishWord.length()>0)
				{
					//englishWord add to words;
					System.out.println(englishWord);
					Map word = new HashMap();
					word.put("cont", englishWord);
					word.put("pos", "n");
					words.add(word);
					//set englishWord = "";
					englishWord = "";
				}
			}			
		}
		return words;  
	}
  
	private boolean isChinese(char c) {
      Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
      if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
              || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
              || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
              || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
          return true;
      }
      return false;
  }  
	
	private boolean isEnglish(String charaString) {

		return charaString.matches("^[a-zA-Z]*");
	}
  
	private ArrayList departWord(List<Map> words){
	  ArrayList<String> highlightWords=new ArrayList();
	  for(Map word : words)
      {        
		  String[] departStr = {"d","e","h","o","p","q","u","wp","m"};
		  if(!Arrays.asList(departStr).contains(word.get("pos").toString())){
			highlightWords.add(word.get("cont").toString());
		  }
      }
	  return highlightWords;
	}

	private LazyList<Model> searchElements(ArrayList<String> highlightWords) {
	  String query = "METADATA_NAME like ?";
	  Object[] params = new Object[highlightWords.size()+1];
	  params[0]=highlightWords.size()>0?highlightWords.get(0):"";
	  int index = 1;
	  for(String highlightWord : highlightWords){		  
		  query = query + "or METADATA_NAME like ?";
		  params[index] = "%" + highlightWord + "%";
  		  ++index;
      }
      LazyList<Model> elementList = Element.where(query,params);
      return elementList;
	}

	private List<Map<String,Object>> sortByParam(List<Map<String,Object>> data) {
	  Collections.sort(data, new Comparator<Map>() {
			public int compare(Map o1, Map o2) {
				String sortParam = null == param("sort")||"" == param("sort")?"count":param("sort");
				Integer count1;
				Integer count2;
				if("count" == sortParam){
					count1 = Integer.valueOf(o1.get("count").toString());
					count2 = Integer.valueOf(o2.get("count").toString());
				}else{
					count1 = (int)(Float.valueOf(o1.get("matchDegree").toString())*10000);
					count2 = (int)(Float.valueOf(o2.get("matchDegree").toString())*10000);
				}
				return count2.compareTo(count1);
			}
		});
	  return data;
  }

	@POST
	public void getMetaDataOption() {
		ArrayList data = new ArrayList();
		
		String elementIDs= param("METADATA_IDS");
		if(null == elementIDs || elementIDs == "")
			output(20001,"参数错误");
		else 
		{
			String[] elementIDArr = elementIDs.split(",");
			for(String elementID : elementIDArr)
			{
				Map node = new HashMap();
				node.put("METADATA_IDENTIFY",elementID);
				
				Map element = new HashMap();
				{
				 String query = "METADATA_IDENTIFY = ?";
				 Object param = elementID;
				 if(Element.count(query,param)>0)
					 element = Element.where(query, param).get(0).toMap();
				}
				
				List<Map<String,Object>> optionList = new ArrayList();
				long countOption = 0;
				{
					Object param = element.get("FIELDCODE_TABLECODE");
					String query = "FIELDCODE_TABLECODE = ?";
					countOption = VOption.count(query,param);
					optionList = VOption.where(query,param).toMaps();
				}
				node.put("COUNT",countOption);
			    node.put("OPTIONS",optionList);
			    data.add(node);				
			}
			output(0,data);
		}				
	}
}
