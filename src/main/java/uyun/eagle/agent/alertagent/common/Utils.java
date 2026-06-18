package uyun.eagle.agent.alertagent.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: yangfei
 * @desc: 工具类
 * @date: created in 2019/3/16 11:09
 * @modifed by:
 */
@Slf4j
public class Utils {

    /**
     * @param fromList
     * @param tClass
     * @param <F>
     * @param <T>
     * @return
     * @desc 将entityList转换成modelList
     */
    public static <F, T> List<T> entityListToModelList(List<F> fromList, Class<T> tClass) {
        if (fromList.isEmpty() || fromList == null) {
            return null;
        }
        List<T> tList = new ArrayList<>();
        for (F f : fromList) {
            T t = entityToModel(f, tClass);
            tList.add(t);
        }
        return tList;
    }

    /**
     * @param entity
     * @param modelClass
     * @param <F>
     * @param <T>
     * @return
     * @desc Entity属性的值赋值到Model
     */
    public static <F, T> T entityToModel(F entity, Class<T> modelClass) {
        log.debug("entityToModel : Entity属性的值赋值到Model");
        Object model = null;
        if (entity == null || modelClass == null) {
            return null;
        }

        try {
            model = modelClass.newInstance();
        } catch (InstantiationException e) {
            log.error("entityToModel : 实例化异常", e);
        } catch (IllegalAccessException e) {
            log.error("entityToModel : 安全权限异常", e);
        }
        BeanUtils.copyProperties(entity, model);
        return (T) model;
    }


    public static String getName() {
        String name = null;
        String basePath = System.getProperty("user.dir");
        log.warn("basePath: {}",basePath);
        basePath = basePath+"/config";
        File file = new File(basePath);
        try {
            if (file.exists() && file.isDirectory()){
                log.warn("config is ok");
                File[] fileList = new File(basePath).listFiles();
                for (File fileName : fileList) {
                    log.warn("configName: {}",fileName.getName());
                    if (fileName.getName().indexOf("-main-base") != -1) {
                        log.warn("config-main: {}",fileName.getName());
                        name = fileName.getName().substring(0, fileName.getName().indexOf("-main-base"));
                        log.warn("name: {}",name);
                    }else {
                        log.warn("not find config-main: {}");
                    }
                }
                if (fileList.length == 0) {
                    return getLocaleName();
                }
            }else {
                log.warn("lib is not ok");
                return getLocaleName();
            }
        } catch (Exception e) {
            log.error("不符合omp安装包目录规范!", e);
        }
        return name;
    }


    /**
     * appCode读取
     *
     * @return
     */
    public static String getLocaleName() {
        String name = null;
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath:*-main-base.yml");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                name = filename.substring(0, filename.indexOf("-main"));
            }
        } catch (Exception e) {
            log.error("不符合yml文件名规范!", e);
        }
        return name;
    }
}
