package app.controllers;

import java.util.List;
import java.util.Map;

import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activejdbc.Base;
import org.javalite.activeweb.annotations.POST;

import app.models.User;

public class UserSvrController extends BasicController {
	
	@POST
	public void login() {
		if(null == param("username") || "" == param("username")){
			output(10001,"用户名为空");
		}
		else if(null == param("password") || "" == param("password"))
		{
			output(10002,"密码为空");
		}
		else 
		{
			String[] param = new String[2]; 
			param[0] = param("username");
			param[1] = param("password");
			String  query = "username = ? and password = ?";
			long count = User.count( query, param);
			if(count > 0)
			{
				LazyList<Model> userList = User.where(query,param);
				Map user = userList.get(0).toMap();
				output(0,user);
			}
			else
			{
				output(10003, "用户名和密码不匹配");
			}
		}
	}
	
	@POST
	public void usersCount() {
		
		if(null == param("sql")){
			output(20001,"参数异常");
		}
	    List<Map> data= Base.findAll(param("sql"));
	    output(0,data);
	}

	@POST
	public void allUsers() {
		
		int pagesize = null == param("pagesize")||"" == param("pagesize")
				? 10 
				:Integer.parseInt(param("pagesize"));
		pagesize = pagesize>1000 ? 10 : pagesize;
       	int offset = null == param("p") || ""  == param("p")
       			?0
       			:(Integer.parseInt(param("p"))-1)*pagesize;
       	String subquery = "?";
       	String params = new String("");
		LazyList<Model> userList = User.where(subquery, params)
			.orderBy("id DESC")
			.limit(pagesize).offset(offset);	
		
	}
}
