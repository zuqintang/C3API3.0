package app.controllers;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import app.models.*;
import app.models.Set;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;
import org.javalite.http.Get;
import org.javalite.http.Http;

public class EPMSvrController extends BasicController
{
	public void index()
	{
		respond("This is EPMSvr");
	}

	private long countSetWhere(){
		String kw = param("keyword").trim();
		long total=Set.count();
		if( !kw.isEmpty() )
		{
			total = Set.count("DS_NAME LIKE ? or DS_CODE LIKE ?",kw,kw);
		}
		return total;
	}

	public void allSets() {
		long total = countSetWhere();
		int pagesize = 10;
		if( ! param("pagesize").isEmpty())
		{
			pagesize = Integer.parseInt(param("pagesize"));
			if(pagesize==0 || pagesize>1000)
			{
				pagesize = 10;
			}
		}
		int offset = 0;
		if( ! param("p").isEmpty())
		{
			offset = (Integer.parseInt(param("p"))-1)*pagesize;
		}
		String kw = param("keyword").trim();
		LazyList<Model> setList=Set.where("DS_NAME LIKE ? or DS_CODE LIKE ?","%"+kw+"%","%"+kw+"%")
					.orderBy("TIMEU desc").limit(pagesize).offset(offset);

		List<Map<String,Object>> Nodes = setList.toMaps();
		for (Map<String,Object> Node:Nodes) {
			Node.put("META_ROWS",Set2Element.count("DS_CODE = ?",Node.get("DS_CODE")));
		}
		output(0, Nodes, Math.ceil(total/pagesize));
	}

	@POST
	public void save()throws Exception {
		Map<String, Object> rs = new HashMap<String, Object>();
		Map params = params1st();
		String ID = param("ID");

		if (ID==null || Integer.parseInt(ID)==0) {
			params.remove("ID");
		}
		Element m = new Element();
		m.fromMap(params);
		if (m.save()) {
			echo(0, m.toMap());
		} else {
			echo(10001, "出错了");
		}
	}

	public void remove() {
		try {
			String ids = param("idsStr");
			int c = Element.delete("ID in("+ids+")");
			echo(0, "删除"+c+"条记录");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@POST
	public void allElements() throws Exception {
//		String kw = param("keyword");
		String kw = java.net.URLDecoder.decode(param("keyword"),"utf-8");
		String query = "";
		LazyList s2eList = Set2Element.where("DS_CODE = ?",param("sid"));
		List<Map> s2eMaps=s2eList.toMaps();
		String[] mids = new String[s2eList.size()];
		int i=0;
		for(Map item :  s2eMaps) {
			mids[i] = item.get("DS_VS_CODE").toString();
			++i;
		}
		String text="";
		if(kw.indexOf("药物途径")==-1)
		{
			String url = "https://api.ltp-cloud.com/analysis/?api_key=N1s4n5W197pGxJWoBHhJHqsoCHADDDuYnyNPTDgP&text="
					+java.net.URLEncoder.encode(kw,"utf-8")
					+"&pattern=pos&format=json";

			if(kw.isEmpty()){
				text = "{\"cont\": \"\"}";
			}
			else
			{
				Get get = Http.get(url);
				if(get.doConnect() == null){
				  text = "{\"cont\": \""+kw+"\"}";
				}else{
					text = get.text();
				}
			}
		}
		else
		{
			text="[{\n" +
					"    \"id\": 0, \n" +
					"    \"cont\": \"药物\", \n" +
					"    \"pos\": \"n\"\n" +
					"   }, \n" +
					"   {\n" +
					"    \"id\": 1, \n" +
					"    \"cont\": \"途径\", \n" +
					"    \"pos\": \"v\"\n" +
					"}]";
		}

		int begin = text.indexOf("{");
		int end = text.lastIndexOf("}")+1;
		text = "["+text.substring(begin,end)+"]";
		Map[] words =JsonHelper.toMaps(text);
		String[] highlightWords=new String[words.length];
		Object[] params =new Object[4+words.length];
		query="METADATA_IDENTIFY in ( ? )"
				+" or METADATA_IDENTIFY = ?"
				+" or FIELDCODE_TABLECODE = ?"
				+" or CREATE_MAN = ?";
		params[0] = mids;
		params[1] = kw;
		params[2] = kw;
		params[3] = kw;
		int index = 4;
		for(Map word : words)
		{
			query = query + " or METADATA_NAME like ?";
			params[index] = "%" + word.get("cont") + "%";
			highlightWords[index-4]=word.get("cont").toString();
			++index;
		}
		long total = Element.count(query,params);
		int pagesize = 10;
		if( null != param("pagesize"))
		{
			pagesize = Integer.parseInt(param("pagesize"));
			if(pagesize<0 || pagesize>1000)
			{
				pagesize = 10;
			}
		}
		int offset = 0;
		if( null != param("p"))
		{
			offset = (Integer.parseInt(param("p"))-1)*pagesize;
		}
		LazyList eleList = Element.where(query,params)
				.limit(pagesize).offset(offset).orderBy("VERSION_DATE DESC");
		List<Map> eleMaps = eleList.toMaps();
		ArrayList data = new ArrayList();
		for(Map eleMap:eleMaps)
		{
			LazyList vopList=VOption.where("FIELDCODE_TABLECODE = ?", eleMap.get("FIELDCODE_TABLECODE"))
					.orderBy("FIELDORDER ASC");
			List<Map> vopMaps = vopList.toMaps();
			if(vopMaps.size()>0)
			{
				eleMap.put("data",vopMaps);
			}

			if( null != eleMap.get("DATA_FEATURE_ID"))
			{
				Unit unit =Unit.findById(eleMap.get("DATA_FEATURE_ID"));
				if(null != unit)
				{
					eleMap.remove("DATA_UNIT");
					eleMap.put("DATA_UNIT",unit.get("DATA_UNIT"));
				}
			}
			eleMap.put("KEYWORDS",highlightWords);
			data.add(eleMap);
		}
		if( null != param("isTotal"))
		{
			output(0, data, total);
		}
		else
		{
			output(0, data, Math.ceil(total/pagesize));
		}
	}

	@POST
	public void eleInfos() throws Exception {
		Map<String,Object> ealias = new HashMap<>();
		String eleCode = java.net.URLDecoder.decode(param("code"),"utf-8");
		List ele = Element.where("METADATA_IDENTIFY = ?", eleCode);
		Object[] eleA = toArray(ele);
		Map eleM = (Map) eleA[0];
		ealias.put("ELEMENT", eleA);

		List options = VOption.where("FIELDCODE_TABLECODE = ?", eleM.get("FIELDCODE_TABLECODE")).orderBy("FIELDORDER asc");
		Object[] optionsA = toArray(options);
		ealias.put("VOPTIONS", optionsA);

		List tplEles = Base.findAll("SELECT a.ID , a.ALIAS, b.NAME FROM tpl_elements a LEFT JOIN templates b on b.ID = a.TEMPLATE_ID WHERE ELEMENT_ID = ?", eleCode);
		ealias.put("TPL_ELEMEMTS", tplEles);

		List sets = Base.findAll("SELECT b.DS_NAME,b.DS_CODE FROM set_vs_element a LEFT JOIN sets b ON b.DS_CODE = a.DS_CODE WHERE a.DS_VS_CODE = ?", eleCode);
		ealias.put("SETS", sets);

		List agencys = Base.findAll("SELECT b.ID,c.AGENCY_ID,c.AGENCY_NAME,d.SUBJECT_ID,d.SUBJECT_NAME FROM tpl_elements a, templates b, edc_agency c, edc_subject d WHERE b.ID = a.TEMPLATE_ID AND b.AGENCY_ID = c.AGENCY_ID AND b.SUBJECT_ID = d.SUBJECT_ID AND  ELEMENT_ID = ? GROUP BY b.ID", eleCode);
		ealias.put("AGENCYS", agencys);

		output(0,ealias);
	}

	@POST
	public void allvoptions() throws Exception {
		Map<String,Object> ealias = new HashMap<>();
		ArrayList array_with_id = new ArrayList();
		int pagesize = 10;
		if( null != param("pagesize"))
		{
			pagesize = Integer.parseInt(param("pagesize"));
			if(pagesize<0 || pagesize>1000)
			{
				pagesize = 10;
			}
		}
		int offset = 0;
		if( null != param("p"))
		{
			offset = (Integer.parseInt(param("p"))-1)*pagesize;
		}
		List<Map> codeList = Base.findAll("SELECT FIELDCODE_TABLECODE,CREATE_MAN FROM voptions GROUP BY FIELDCODE_TABLECODE LIMIT ? OFFSET ?", pagesize, offset);
		Integer total = Base.findAll("SELECT FIELDCODE_TABLECODE,CREATE_MAN FROM voptions GROUP BY FIELDCODE_TABLECODE").size();
		for(int i=0; i<codeList.size(); i++)
		{
			Map<String,Object> vopM = new HashMap<>();
			List options = VOption.where("FIELDCODE_TABLECODE = ?", codeList.get(i).get("FIELDCODE_TABLECODE")).orderBy("FIELDORDER asc");
			Object[] optionsA = toArray(options);
			vopM.put("FIELDCODE_TABLECODE",codeList.get(i).get("FIELDCODE_TABLECODE"));
			vopM.put("CREATE_MAN",codeList.get(i).get("CREATE_MAN"));
			vopM.put("VOPTIONS",optionsA);
			List eleList = Element.where("FIELDCODE_TABLECODE = ?",codeList.get(i).get("FIELDCODE_TABLECODE"));
			Object[] eleA = toArray(eleList);
			if(eleA.length == 0){
				vopM.put("METADATAFIELD_NAME",null);
			}else{
				Map eleM = (Map) eleA[0];
				vopM.put("METADATAFIELD_NAME",eleM.get("METADATAFIELD_NAME"));
			}
			array_with_id.add(vopM);
		}

		output(0, array_with_id, total);
	}

	@POST
	public void vopInfos() throws Exception {
		Map<String,Object> ealias = new HashMap<>();
		String tableCode = java.net.URLDecoder.decode(param("code"),"utf-8");
		List eleList = Element.where("FIELDCODE_TABLECODE = ?",tableCode);
		Object[] eleA = toArray(eleList);
		if(eleA.length == 0){
			ealias.put("METADATAFIELD_NAME",null);
		}else{
			Map eleM = (Map) eleA[0];
			ealias.put("METADATAFIELD_NAME",eleM.get("METADATAFIELD_NAME"));
		}
		List options = VOption.where("FIELDCODE_TABLECODE = ?", tableCode).orderBy("FIELDORDER asc");
		Object[] optionsA = toArray(options);
		ealias.put("FIELDCODE_TABLECODE", tableCode);
		ealias.put("VOPTIONS",optionsA);
		output(0, ealias);
	}

	@POST
	public void setInfos() throws Exception {
		Map<String,Object> ealias = new HashMap<>();
		String setCode = param("code");
		List setList = Set.where("DS_CODE = ?",setCode);
		Object[] setA = toArray(setList);
		ealias.put("SET",setA);
		List eles = Base.findAll("SELECT a.DS_CODE,a.DS_NAME,b.METADATA_NAME,b.METADATA_IDENTIFY,b.DATA_META_DATATYPE,b.FIELDCODE_TABLECODE FROM sets a, elements b, set_vs_element c WHERE a.DS_CODE = c.DS_CODE AND b.METADATA_IDENTIFY = c.DS_VS_CODE AND a.DS_CODE = ?", setCode);
		ealias.put("ELEMENTS", eles);
		output(0, ealias);
	}

	@POST
	public void elesTable() throws Exception {
		String setCode = param("code");
		int pagesize = 10;
		if( null != param("pagesize"))
		{
			pagesize = Integer.parseInt(param("pagesize"));
			if(pagesize<0 || pagesize>1000)
			{
				pagesize = 10;
			}
		}
		int offset = 0;
		if( null != param("p"))
		{
			offset = (Integer.parseInt(param("p"))-1)*pagesize;
		}
		Integer total = Base.findAll("SELECT a.DS_CODE,a.DS_NAME,b.METADATA_NAME,b.METADATA_IDENTIFY,b.DATA_META_DATATYPE,b.FIELDCODE_TABLECODE FROM sets a, elements b, set_vs_element c WHERE a.DS_CODE = c.DS_CODE AND b.METADATA_IDENTIFY = c.DS_VS_CODE AND a.DS_CODE = ?", setCode).size();
		List eles = Base.findAll("SELECT a.DS_CODE,a.DS_NAME,b.METADATA_NAME,b.METADATA_IDENTIFY,b.DATA_META_DATATYPE,b.FIELDCODE_TABLECODE FROM sets a, elements b, set_vs_element c WHERE a.DS_CODE = c.DS_CODE AND b.METADATA_IDENTIFY = c.DS_VS_CODE AND a.DS_CODE = ? limit ? offset ?", setCode, pagesize, offset);
		output(0, eles, total);
	}

	@POST
	public void setSave () throws Exception {
		Map<String,Object> ealias = new HashMap<>();
		Date date=new Date();
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=format.format(date);
		if(null == param("DS_CODE") || param("DS_CODE") == ""){
			ealias.put("YEAR_VERSION",param("YEAR_VERSION"));
			ealias.put("DS_NAME",param("DS_NAME"));
			ealias.put("DS_GROUP",param("DS_GROUP"));
			ealias.put("DIS_GROUP",param("DIS_GROUP"));
			ealias.put("TIMEI",time);
			ealias.put("TIMEU",time);
			Set set =new Set();
			set.fromMap(ealias);
			set.save();
			int GETID = Integer.parseInt(set.getId().toString());
			Set e = Set.findFirst("ID = ?", GETID);
			e.set("DS_CODE",GETID,"TIMEU",time).saveIt();
			output(0, ealias);
		}else{
			Set e = Set.findFirst("DS_CODE = ?", param("DS_CODE"));
			e.set("YEAR_VERSION",param("YEAR_VERSION"),"DS_NAME",param("DS_NAME"),"DS_GROUP",param("DS_GROUP"),"DIS_GROUP",param("DIS_GROUP"),"TIMEU",time).saveIt();
			output(0, ealias);
		}
	}
}
