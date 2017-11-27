package app.controllers;

import app.models.Formula;
import app.models.FormulasDe;
import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;
import org.javalite.activeweb.annotations.POST;
import org.javalite.common.JsonHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FormulaSvrController extends BasicController {
    public void index() {
        respond("This is FormulaSvr.");
    }

    @POST
    public void allFormulas() {
        if(null == param("tid") || param("tid") == "" )
            output(20001,"参数异常");
        LazyList<Model> formulaList = Formula.where("TID = ?",param("tid"));
        List<Map<String,Object>> data = formulaList.toMaps();
        output(0,data);
    }

    @POST
    public void allFormulasDes() {
        if(null == param("ID")||param("ID") == "") {
            LazyList<Model> formulaDesList = FormulasDe.findAll();
            List<Map<String, Object>> data = formulaDesList.toMaps();
            output(0, data);
        }else{
            String query = "ID = ? ";
            Object param = param("ID");
            LazyList<Model> formulaDesList = FormulasDe.where(query,param);
            List<Map<String, Object>> data = formulaDesList.toMaps();
            output(0, data);
        }
    }

    public void model() {
        if(null == param("id") || "" == param("id"))
            output(20001,"参数异常");
        Formula formula = Formula.findById(param("id"));
        Map node = formula.toMap();
        if(null == node.get("SYMBOLS")){
            node.put("SYMBOLS","");
        }else
            node.put("SYMBOLS", JsonHelper.toMaps(node.get("SYMBOLS").toString()));
    }

    @POST
    public void save() {
        Map param = params1st();
        Formula formula = new Formula();
        String ID = param("ID");
        if (ID==null || Integer.parseInt(ID)==0) {
            param.remove("ID");
        }
        formula.fromMap(param);
        if(formula.save()) {
            output(0, param);
        }else {
            output(20001, "保存失败");
        }
    }
}
