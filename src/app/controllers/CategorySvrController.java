package app.controllers;

import app.models.ZzTree;
import org.javalite.activejdbc.LazyList;
import org.javalite.activeweb.annotations.POST;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategorySvrController extends BasicController {
    public void index() {
        respond("This is CategorySvr");
    }

    @POST
    public void tree() {
        LazyList<ZzTree> zzTreeList = ZzTree.findAll().orderBy("SORTER DESC");
        List<Map<String,Object>> zzTrees = zzTreeList.toMaps();
        ArrayList data = new ArrayList();
        Map rootNode = new HashMap();
        rootNode.put("id",0);rootNode.put("text","全部");rootNode.put("pId",-1);
        data.add(rootNode);
        for(Map zzTree : zzTrees){
            Map node = new HashMap();
            node.put("id",zzTree.get("ID"));
            node.put("pid",zzTree.get("PID"));
            node.put("text",zzTree.get("ZNAME"));
            data.add(node);
        }
        output(0, data);
    }
}
