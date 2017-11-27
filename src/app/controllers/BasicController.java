package app.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.javalite.activejdbc.Base;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.AppController;

import com.fasterxml.jackson.databind.ObjectMapper;

public class BasicController extends AppController {
		
	public BasicController() {
		super();
		Base.open("com.mysql.jdbc.Driver", "jdbc:mysql://192.168.2.117/jhedc?useUnicode=true&zeroDateTimeBehavior=convertToNull&characterEncoding=UTF-8", "edc", "123456");
	}
	
	public Object[] toArray(List <Model> inList)
	{
		Object[] res = new Object[inList.size()];
		for(int i=0; i<inList.size(); i++)
		{
			Model m = inList.get(i);
			res[i] = m.toMap();
		}
        return res;
	}
	
	public void toJSON(Object in)
	{
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(in);
			respond(json).contentType("text/plain;charset=UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void echo(int code, Object data)
	{
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("code", code);
		res.put("data", data);
		
		try {
			ObjectMapper mapper = new ObjectMapper();
	        String json = mapper.writeValueAsString(res);
			respond(json).contentType("text/plain;charset=UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void output( int code, Object data, double... countPage){
		Map<String, Object> res = new HashMap<String, Object>();
		res.put("code", code);
		res.put("data", data);
		res.put("countPage",countPage);
		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(res);
			respond(json).contentType("text/plain;charset=UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public TreeMap<String, String> cookColumn(String field , String title)
	{
		TreeMap<String, String> m = new TreeMap();
		m.put("field", field);
		m.put("title", title);
		return m;
	}
}
