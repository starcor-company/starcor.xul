package com.starcor.xulapp.utils;

import com.starcor.xulapp.model.XulDataService;

/**
 * Created by hy on 2015/9/25.
 */
public interface XulExecutable {
	boolean exec(XulDataService.Clause clause);
}
