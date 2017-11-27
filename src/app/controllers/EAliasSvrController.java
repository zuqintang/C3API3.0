package app.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import app.models.*;
import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class EAliasSvrController extends BasicController
{
	public void index()
	{
		respond("This is EAliasSvr");
	}

	@POST
	public void aliasSave () throws Exception {
		Map params = params1st();
		Map<String,Object> ealias = new HashMap<>();
		Map<String,Object> newEalias = new HashMap<>();
		String ALIAS = java.net.URLDecoder.decode(param("ALIAS"),"utf-8");
		Date date=new Date();
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=format.format(date);

		ealias.put("TEMPLATE_ID",param("TEMPLATE_ID"));
		ealias.put("ALIAS",ALIAS);
		ealias.put("DATATYPE",param("DATATYPE"));
		ealias.put("ISBIND",1);
		ealias.put("TIMEI",time);
		ealias.put("TIMEU",time);
		TplElement tplelement =new TplElement();
		tplelement.fromMap(ealias);
		tplelement.save();

		int GETID = Integer.parseInt(tplelement.getId().toString());
		String strId = tplelement.getId().toString();
		TplElement e = TplElement.findFirst("INT_ID = ?", GETID);
		e.set("ELEMENT_ID", strId, "ID", strId+"_"+param("TEMPLATE_ID").toString()).saveIt();

		ArrayList array_with_id = new ArrayList();
		Map[] voptions =JsonHelper.toMaps(params.get("voptions").toString());
		for (Map<String,Object> option: voptions) {
			option.put("ISBIND",1);
			option.put("EALIAS_ID",strId+"_"+param("TEMPLATE_ID").toString());
			option.put("TEMPLATE_ID",param("TEMPLATE_ID"));
			option.put("TIMEI",time);
			option.put("TIMEU",time);
			TplOption tploption =new TplOption();
			tploption.fromMap(option);
			tploption.save();
			array_with_id.add(option);
		}
		ealias.put("INT_ID",GETID);
		ealias.put("ID",strId+"_"+param("TEMPLATE_ID").toString());
		if(array_with_id.size() >0 ){
			ealias.put("voptions",array_with_id);
		}
		output(0,ealias);
	}

	@POST
	public void bindEle () throws Exception {
		Map<String,Object> ealias = new HashMap<>();
		String strId = param("INT_ID").toString();
		int intId = Integer.parseInt(strId);
		List<Model> options = TplOption.where("EALIAS_ID = ?", strId+"_"+param("TEMPLATE_ID").toString()).orderBy("SORTER");
		Object[] optionA = toArray(options);
		long num1 = TplOption.count("EALIAS_ID = ?",strId+"_"+param("TEMPLATE_ID").toString());

		List eleList = Element.where("METADATA_IDENTIFY = ?",param("ELEMENT_ID"));
		Object[] eleA = toArray(eleList);
		ealias.put("ELEMENTS",eleA);
		Map eleM = (Map) eleA[0];

		List vopList = VOption.where("FIELDCODE_TABLECODE = ?",eleM.get("FIELDCODE_TABLECODE")).orderBy("FIELDORDER");
		Object[] voptionA = toArray(vopList);
		ealias.put("VOPTIONS",voptionA);
		long num2 = VOption.count("FIELDCODE_TABLECODE = ?",eleM.get("FIELDCODE_TABLECODE"));

		if(num1 != num2){
			output(1,"值域项不匹配，请更换数据元绑定！");
		}else{
			TplElement e = TplElement.findFirst("INT_ID = ?", intId);
			e.set("ELEMENT_ID", param("ELEMENT_ID"), "ID", param("ID")).saveIt();
			for(int i=0; i<optionA.length; i++)
			{
				TreeMap one = (TreeMap)optionA[i];
				TreeMap vops = (TreeMap)voptionA[i];
				TplOption uE = TplOption.findFirst("ID = ?", one.get("ID"));
				uE.set("VOPTION_ID", vops.get("ID"), "EALIAS_ID", param("ID"), "AVALUE", vops.get("FIELDCODE_VALUE")).saveIt();
			}
			output(0,ealias);
		}
	}

	@POST
	public void addAlias () throws Exception {
		String id = param("id");
		Long num = TplOption.count("EALIAS_ID = ?", id);
		List<TplOption> optionsList = TplOption.find("EALIAS_ID = ?", id);
		TplOption option = optionsList.get(0);
		Map<String,Object> ealias = new HashMap<>();
		Date date=new Date();
		DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time=format.format(date);
		ealias.put("EALIAS_ID",id);
		ealias.put("TEMPLATE_ID",option.get("TEMPLATE_ID"));
		ealias.put("ALIAS","新增值域项");
		ealias.put("AVALUE",num);
		ealias.put("CVALUE",0);
		ealias.put("SORTER",num);
		ealias.put("ISBIND",1);
		ealias.put("TIMEI",time);
		ealias.put("TIMEU",time);
		ealias.put("STATUS",2);
		TplOption tploption =new TplOption();
		tploption.fromMap(ealias);
		tploption.save();
		output(0,ealias);
	}

	private String computeInputType(String datatype) throws Exception {
		String inputtype = "";
		if(datatype == "TA"){
			inputtype="textarea";
		}else if(datatype == "L"){
			inputtype="radio";
		}else if(datatype == "S3"){
			inputtype="checkbox";
		}else if(datatype == "S2" || datatype == "S4" || datatype == "ADR"){
			inputtype="select";
		}else{
			inputtype="input";
		}
		return inputtype;
	}
	@POST
	public void save () throws Exception {
		Map<String, Object> rs = new HashMap<String, Object>();
		Map params = params1st();
		params.put("inputtype",computeInputType(params.get("datatype").toString()));
		String id = param("id");
		if (null == id || id == "") {
			output(20001,"参数异常");
		}
		String[] ids = id.split("_");
		if (ids.length<2) {
			output(11002,"别元格式错误:"+id);
		}
		Template template = Template.findById(ids[1]);
		Map<String,Object> Node = template.toMap();
		int zid= Integer.parseInt(Node.get("ZID").toString());
		int pid= Integer.parseInt(Node.get("PID").toString());
		LazyList<TplElement> tplElementList = TplElement.where("ID = ?",id);
		Map<String, Object> ealias = new HashMap<>();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = format.format(System.currentTimeMillis());
		TplElement tplElement;
		if(0 == tplElementList.size()){
			tplElement = new TplElement();
			ealias.put("ID", id);
			ealias.put("DATATYPE", null == param("datatype") ? "" : param("datatype"));
			ealias.put("INPUT_TYPE",params.get("inputtype"));
			ealias.put("ALIAS", params.get("alias"));
			ealias.put("PROJECT_ID", pid);
			ealias.put("ZID",zid);
			ealias.put("ELEMENT_ID", ids[0]);
			ealias.put("TEMPLATE_ID", ids[1]);
			ealias.put("TIMEI", time);
			ealias.put("TIMEU", time);
		}else {
			tplElement = tplElementList.get(0);
			ealias.put("TEMPLATE_ID", ids[1]);
			ealias.put("ELEMENT_ID", ids[0]);
			ealias.put("TIMEU", time);
			ealias.put("PROJECT_ID", pid);
			ealias.put("ZID",zid);
			ealias.put("DATATYPE", null == param("datatype") ? "" : param("datatype"));
			ealias.put("ALIAS", params.get("alias"));
		}
		if(null != params.get("voptions") && "" != params.get("voptions")) {
			ArrayList array_with_id = new ArrayList();
			Map[] voptions = JsonHelper.toMaps(params.get("voptions").toString());
			for (Map<String, Object> voption : voptions) {
				Map data = new HashMap();
				data.put("ALIAS", voption.get("alias"));
				data.put("AVALUE", voption.get("avalue"));
				data.put("SORTER", voption.get("sorter"));
				data.put("STATUS", voption.get("status"));
				data.put("TIMEU", time);
				if (null == voption.get("id")) {
					data.put("EALIAS_ID", id);
					data.put("VOPTION_ID", voption.get("voption_id"));
					data.put("TEMPLATE_ID", ids[1]);
					data.put("TIMEI", time);
				} else {
					data.put("ID", voption.get("id"));
					data.put("CVALUE", voption.get("cvalue"));
				}
				TplOption tploption = new TplOption();
				tploption.fromMap(data);
				tploption.save();
				ealias.put("HELE",tploption.get("EALIAS_ID"));
				if(ealias.get("HVAL") != null)
					ealias.put("HVAL",tploption.getId());
				data.put("ID", tploption.getId());
				array_with_id.add(data);
			}
			if (array_with_id.size() > 0) {
				ealias.put("voptions", array_with_id);
			}
		}
		tplElement.fromMap(ealias);
		if(tplElement.save()) {
			output(0, ealias);
		}else{
			output(20001,"数据元保存失败");
		}
	}

	@POST
	public void delete() throws Exception {
		if(null == param("ids") || param("ids") == ""){
			output(20001, "参数异常");
		}
		String ids = param("ids");
		int elementNum = TplElement.delete("ID in ("+ ids +")");
		int optionNum = TplOption.delete("EALIAS_ID in (" + ids +")");

		output(0,"删除了"+elementNum+ "条数据元，"+optionNum+"值域项");
	}

	@POST
	public void model() throws Exception {
		String ID = param("id");
		if(null == ID || "" == ID)
			output(20001,"参数异常");

		List<Model> tplElementList = TplElement.where("ID = ?", ID);
		Map data = tplElementList.get(0).toMap();
		if(tplElementList.size() == 0)
			output(130001, "库中没有查到对应的元");
		
		LazyList tplOptionList = TplOption.where("EALIAS_ID = ?", ID).orderBy("SORTER ASC");
		List<Map> tplOptions = tplOptionList.toMaps();
		ArrayList aliasNames = new ArrayList();
		for(Map tplOption :tplOptions){
			VOption vOption = VOption.findById(tplOption.get("VOPTION_ID"));
			if(vOption != null)
				aliasNames.add(vOption.get("FIELDCODE_VALUE_CN_NAME").toString());
		}		
		
		data.put("data", tplOptions);
		data.put("voptionName", aliasNames);
		output(0, data);
	}

	@POST
	public void saveRange() throws Exception {
		String tplElementID =param("ID");
		Map tplEle = new HashMap();
		
		if(TplElement.count("ID = ?",tplElementID)>0){
			tplEle = TplElement.where("ID = ?",tplElementID).get(0).toMap();
			if(null != param("MAX_VALUE") && "" != param("MAX_VALUE"))
				tplEle.put("MAX_VALUE",param("MAX_VALUE"));
			if(null != param("MIN_VALUE") && "" != param("MIN_VALUE"))
				tplEle.put("MIN_VALUe", param("MIN_VALUE"));
			if(null != param("MAX_NORMALVALUE") && "" != param("MAX_NORMALVALUE"))
				tplEle.put("MAX_NORMALVALUE", param("MAX_NORMALVALUE"));
			if(null != param("MIN_NORMALVALUE") && "" != param("MIN_NORMALVALUE"))
				tplEle.put("MIN_NORMALVALUE", param("MIN_NORMALVALUE"));
			
			TplElement tplElement = new TplElement();
			tplElement.fromMap(tplEle);
			if(tplElement.save())
				output(0,"数据范围保存成功");
			else
				output(20001,"数据范围保存失败");
		}
		else
			output(20001,"模版数据元不存在");
		
		
	}
}
