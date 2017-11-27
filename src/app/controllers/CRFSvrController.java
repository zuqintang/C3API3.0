package app.controllers;

import app.models.*;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.PATCH;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static jdk.internal.dynalink.support.NameCodec.decode;

public class CRFSvrController extends BasicController {

    public void index() {
        respond("This is CRFSvr");
    }

    @POST
    public void model() throws Exception {
        if(null == param("id")||param("id")=="")
        {
            output(20001,"参数异常");
        }
        Template template = Template.findById(Integer.parseInt(param("id").toString()));
        Map data = template.toMap();
        ZzTree zzTreeProject = ZzTree.findById(template.get("PID").toString());
        if(null!=zzTreeProject)
            data.put("PROJECT_NAME", zzTreeProject.get("ZNAME").toString().trim());
        ZzTree zzTreeHospital = ZzTree.findById(template.get("HID").toString());
        if(null!=zzTreeHospital)
            data.put("HOSPITAL_NAME", zzTreeHospital.get("ZNAME").toString().trim());
        output(0, data);
    }

    @POST
    public void htmlStr() throws Exception {
        if (null == param("id")) {
            output(20001, "参数异常");
        }
        int id = Integer.parseInt(param("id"));
        String fileURL = cookFileURL(id);
        String htmlStr = "";
        File file = new File(fileURL);
        if (file.exists()) {
            htmlStr = fileToString(fileURL);
        }

        output(0, htmlStr);
    }

    @POST
    public void delete() throws Exception {

        String ids =param("ids");
        if(ids.isEmpty()){
            output(20001,"参数异常");
        }
        Template.delete("ID in ("+ ids + ")");
        TplElement.delete("TEMPLATE_ID in ("+ ids + ")");
        output(0,"删除成功");
    }

    @POST
    public void newAllTemplates() throws Exception {
        long total = 0;
        String query = "";
        Object[] params =new Object[3];
        query = "NAME like ? or ID = ? or SUBJECT_ID = ?";
        params[0]= "%"+param("keyword")+"%";
        params[1]= param("ID");
        params[2]= param("SUBJECT_ID");
        total = Template.count(query,params);
        int pagesize = 10;
        if(null != param("pagesize")) pagesize=Integer.parseInt(param("pagesize"));
        int offset = 0;
        if(null != param("offset")) offset=(Integer.parseInt(param("p"))-1)*pagesize;
        List resList = Template.where(query,params).offset(offset).limit(pagesize).orderBy("ID DESC");
        Object[] templates= toArray(resList);
        ArrayList newNodes = new ArrayList();
        for (Object template:templates) {
            TreeMap node = (TreeMap) template;
            long TEMPLATE_NUM = TplElement.count("TEMPLATE_ID = ?",node.get("ID"));
            node.put("TEMPLATE_NUM",TEMPLATE_NUM);
            Subject subject = Subject.findById(node.get("SUBJECT_ID"));
            String SUBJECT_NAME = subject.get("SUBJECT_NAME").toString();
            String AGENCY_ID = subject.get("AGENCY_ID").toString();
            String AGENCY_NAME = Agency.findById(AGENCY_ID).get("AGENCY_NAME").toString();
            node.put("SUBJECT_NAME",SUBJECT_NAME);
            node.put("AGENCY_NAME",AGENCY_NAME);
            node.put("AGENCY_ID",AGENCY_ID);
            node.remove("HTMLSTR");
            node.remove("LOGICJSON");
            newNodes.add(node);
        }
        Map data = new HashMap();
        data.put("total",total);
        data.put("rows",newNodes);
        if(null !=param("isBoostrap")){
            output(0, newNodes, Math.ceil(total/pagesize));
        }else{
            toJSON(data);
        }
    }

    @POST
    public void newInsertOrUpdate() throws Exception {
        Map params = params1st();
        if(param("NAME").isEmpty()) output(20002, "名字不能为空");
        DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String VERSION_DATE = format.format(param("VERSION_DATE"));
        params.remove("VERSION_DATE");
        params.put("VERSION_DATE",VERSION_DATE);
        String ID = param("ID");
        if (ID==null || Integer.parseInt(ID)==0) {
            params.remove("ID");
        }
        Template m = new Template();
        m.fromMap(params);
        if (m.save()) {
            output(0, "更新成功");
        } else {
            output(20005, "更新失败");
        }
    }

    @POST
    public void save() throws Exception {
        String request = decode(param("tplData"));
        if(null == request) output(20001, "参数异常");
        String result=new String(Base64.decodeBase64(request),"utf-8");
        Map tplData = JsonHelper.toMap(result);
        if (null == tplData.get("id")) {
            output(20002, "参数异常,ID不能为空");
        }
        if(null != tplData.get("elesInfo")){
            Map[] eleInfos = JsonHelper.toMaps(tplData.get("elesInfo").toString());
            for(Map item :eleInfos)
            {
                TplElement tplElement;
                LazyList<TplElement> tplElementList = TplElement.where("ID = ?",item.get("ID"));
                if(tplElementList.size()>0)
                    tplElement = tplElementList.get(0);
                else
                    tplElement = new TplElement();
                Map t = new HashMap();
                t.put("TINC", item.get("TINC"));
                t.put("SORTER", item.get("SORTER"));
                tplElement.fromMap(t);
                tplElement.save();
            }
        }
        String id = tplData.get("id").toString();
        String userName= tplData.get("userName").toString();
        String htmlStr = tplData.get("htmlStr").toString();
        String logicJSON = tplData.get("dataJSON").toString();
        String fileURL = saveFileURL(id, userName);
        boolean resTpl = stringToFile(fileURL, htmlStr);
        if(!resTpl) output(200010, "系统错误");
        String previewStr = tplData.get("previewStr").toString();
        Template template= Template.findById(id);
        Map t = template.toMap();
        t.remove("HTMLSTR");t.remove("LOGICJSON");
        t.put("HTMLSTR",previewStr);t.put("LOGICJSON",logicJSON);
        template.fromMap(t);
        template.save();
        String previewURL = savePreviewURL(id);
        
        boolean resDw = stringToFile(previewURL, previewStr);
        if(resDw){
            output(0, "保存成功");
        }else{
            output(200010, "系统错误");
        }
    }

    @POST
    public void getSelectTpls() throws Exception {
        LazyList<Model> subjectList = ZzTree.findAll();
        List<Map<String,Object>> subjects = subjectList.toMaps();
        ArrayList data = new ArrayList();
        for(Map subject: subjects){
            Map node = new HashMap();
            node.put("SUBJECT_NAME",subject.get("ZNAME").toString());
            node.put("SUBJECT_ID",subject.get("ID").toString());
            ZzTree zzTreeHospital=ZzTree.findById(subject.get("PID"));
            if(null!=zzTreeHospital) {
                node.put("AGENCY_NAME", zzTreeHospital.get("ZNAME").toString());
            }else{
                node.put("AGENCY_NAME","");
            }
            data.add(node);
        }
        output(0,data);
    }

    @POST
    public void temName() {
        if(null ==param("id")||param("id")==""){
            output(20001,"参数异常");
        }
        Template template = Template.findById(param("id"));
        String data = template.get("NAME").toString();
        output(0,data);
    }

    @POST
    public void allTemplates() {
        String query="NAME like ? or ID = ? or ZID =? or PID = ? or HID = ?";
        Object[] param = new Object[5];
        param[0]="%"+param("keyword")+"%";
        param[1]=param("keyword");
        param[2]=param[3]=param[4]=param("zid");

        long total = Template.count(query,param);
        int pagesize = 10;
        if(null != param("pagesize")) {
            pagesize = Integer.parseInt(param("pagesize"));
            if(pagesize<1 || pagesize>1000) {
                pagesize = 10;
            }
        }
        int offset = 0;
        if(null != param("p")) {
            offset = (Integer.parseInt(param("p"))-1)*pagesize;
        }

        LazyList<Model> templateList;
        if(null != param("sorter") && null != param("desc")) {
            templateList = Template.where(query, param)
                    .limit(pagesize)
                    .offset(offset)
                    .orderBy(param("sorter")+" "+param("desc"));
        }else {
            templateList = Template.where(query, param)
                    .limit(pagesize)
                    .offset(offset);
        }

        List<Map<String,Object>> templates = templateList.toMaps();
        ArrayList data = new ArrayList();
        for(Map template : templates){
            Map node = new HashMap();
            node=template;
            ZzTree zzTreeZ =ZzTree.findById(template.get("ZID"));
            if(null != zzTreeZ)
                node.put("ZNAME",zzTreeZ.get("ZNAME"));
            ZzTree zzTreeProject = ZzTree.findById(template.get("PID"));
            if(null != zzTreeProject)
                node.put("PROJECT_NAME",zzTreeProject.get("ZNAME"));
            ZzTree zzTreeHospital = ZzTree.findById(template.get("HID"));
            if(null != zzTreeHospital)
                node.put("HOSPITAL_NAME",zzTreeHospital.get("ZNAME"));
            node.remove("HTMLSTR");
            node.remove("LOGICJSON");
            data.add(node);
        }

        output(0, data, Math.ceil(total/pagesize));
    }

    @POST
    public void searchElementAlias() {
        if(null == param("id") || param("id") == "")
            output(20001, "参数异常");
        Template template =Template.findById(param("id"));
        LazyList<Model> tplElementList=TplElement.where("PROJECT_ID = ?",template.get("PID"))
            .orderBy("TEMPLATE_ID DESC");
        List<Map<String,Object>> data=tplElementList.toMaps();
        output(0, data);
    }

    private String  saveFileURL(String id,String userName) {
        long  timestamp = System.currentTimeMillis();
        String directory = System.getProperty("user.dir")+"/templates/"+id;
        File file=new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        String filename = "tpl_"+id+"_newest.html";
        File file2=new File(directory,filename);
        if (file2.exists()) {
            String otherName = "tpl_"+id+"_"+timestamp+"_"+userName+".html";
            File file3 = new File(directory,otherName);
            file2.renameTo(file3);
        }
        return directory+"/"+filename;
    }

    private boolean stringToFile(String fileUrl, String htmlStr){
        try {
            OutputStreamWriter out = new OutputStreamWriter(
                    new FileOutputStream(fileUrl),"UTF-8");
            out.write(htmlStr);
            out.flush();
            out.close();
            return true;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
        return false;
    }

    private String savePreviewURL(String id) {
        String directory = System.getProperty("user.dir")+"/templates/"+id;
        String filename = "public_"+id+".html";
        File file=new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        File file2=new File(directory,filename);
        if (!file2.exists()) {
            try {
                file2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory+"/"+filename;
    }

    private String cookFileURL(int id) {
        String directory = System.getProperty("user.dir")+"/templates/"+id;
        File file=new File(directory);
        if (file.exists()) {
            return directory+"/tpl_"+id+"_newest.html";
        }
        return directory+"/templates_"+id+".html";
    }

    private String fileToString(String filename) throws IOException {
        InputStreamReader input = new InputStreamReader(new FileInputStream(filename),"UTF-8");
        BufferedReader reader = new BufferedReader(input);
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null)
        {
            builder.append(line);
        }

        reader.close();
        return builder.toString();
    }
    
    @POST
    public void viewHtmlStr() throws Exception {
    	 if (null == param("id")) {
             output(20001, "参数异常");
         }
         int id = Integer.parseInt(param("id"));
         String fileURL = cookViewFileURL(id);
         String htmlStr = "";
         File file = new File(fileURL);
         if (file.exists()) {
            htmlStr = fileToString(fileURL);
    		Document doc = Jsoup.parse(htmlStr);
    		doc.select(".c-toolbar").remove();
    		Elements newInputlines = doc.select(".c-input");
    		for(org.jsoup.nodes.Element inputline : newInputlines )
    		{
    			String name = "";
    			if("label" == inputline.child(0).tagName())
    				name = inputline.child(0).child(0).attr("name");
    			else
    				name = inputline.child(0).attr("name");
    			inputline.empty();
    			inputline.append("<span class=\"spanRigth\" name=\""+name+"\">："+name+"</span>");
    			inputline.select(".content").removeClass("c-indent");
    			inputline.removeClass("s-win");
    			inputline.select(".dropdown").remove();
    			inputline.select(".c-grid").removeAttr("onmouseover");
    			inputline.select(".c-grid").removeAttr("onclick");
    		}
    		output(0,doc.body().children().toString());
    	}
    }

    @POST
    public void saveViewHtmlStr() throws Exception {
    	String request = decode(param("tplData"));
        if(null == request) output(20001, "参数异常");
        String result=new String(Base64.decodeBase64(request),"utf-8");
        Map tplData = JsonHelper.toMap(result);
        if (null == tplData.get("id")) {
            output(20002, "参数异常,ID不能为空");
        }
        String id = tplData.get("id").toString();
        String userName= tplData.get("userName").toString();
        String viewHtmlStr = tplData.get("viewHtmlStr").toString();
        String viewPreHtmlStr = tplData.get("viewPreHtmlStr").toString();
        
        //编辑模版Html文件保存
        String fileViewURL =saveViewHtmlURL(id,userName);         
        boolean resTpl = stringToFile(fileViewURL, viewHtmlStr);
        if(!resTpl) output(200010, "系统错误");
        
        Template template= Template.findById(id);
        Map t = template.toMap();
        t.put("VIEWHTMLSTR",viewPreHtmlStr);
        template.fromMap(t);
        template.save();
        
        //预览模版Html文件保存
        String fileViewPreURL = savePreViewURL(id);        
        boolean resDw = stringToFile(fileViewPreURL, viewPreHtmlStr);
        if(resDw)
            output(0, "保存成功");
        else
            output(200010, "系统错误");
    }

    private String cookViewFileURL(int id) {
        String directory = System.getProperty("user.dir")+"/templates/"+id;
        File file=new File(directory);
        if (file.exists()) {
            return directory+"/view_tpl_"+id+"_newest.html";
        }
        return directory+"/templates_"+id+".html";
    }

    private String savePreViewURL(String id) {
		// TODO Auto-generated method stub
		
		String directory = System.getProperty("user.dir")+"/templates/"+id;
        String filename = "public_view_"+id+".html";
        File file=new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        File file2=new File(directory,filename);
        if (!file2.exists()) {
            try {
                file2.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return directory+"/"+filename;
	}

	private String saveViewHtmlURL(String id, String userName) {
		// TODO Auto-generated method stub
		
		long  timestamp = System.currentTimeMillis();
        String directory = System.getProperty("user.dir")+"/templates/"+id;
        File file=new File(directory);
        if (!file.exists()) {
            file.mkdirs();
        }
        String filename = "view_tpl_"+id+"_newest.html";
        File file2=new File(directory,filename);
        if (file2.exists()) {
            String otherName = "view_tpl_"+id+"_"+timestamp+"_"+userName+".html";
            File file3 = new File(directory,otherName);
            file2.renameTo(file3);
        }
        return directory+"/"+filename;
	}
}

