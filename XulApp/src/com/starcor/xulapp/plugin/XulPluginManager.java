package com.starcor.xulapp.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * Created by hy on 2016/5/12.
 */
public class XulPluginManager {

	public static class XulPluginInfo {
		public XulPlugin _plugin;
		public File _path;
		ZipFile _pluginPackage;
		ClassLoader _classLoader;

		public boolean init(File path) {
			_path = path;

			try {
				File parentFile = path.getParentFile();
				_classLoader = new DexClassLoader(_path.getAbsolutePath(), parentFile.getAbsolutePath(), null, getClass().getClassLoader());

				_pluginPackage = new ZipFile(_path);
				ZipEntry entry = _pluginPackage.getEntry("plugin.txt");
				BufferedReader reader = new BufferedReader(new InputStreamReader(_pluginPackage.getInputStream(entry)));
				String pluginClassName = reader.readLine();
				reader.close();

				Class<?> pluginClass = _classLoader.loadClass(pluginClassName);
				_plugin = (XulPlugin) pluginClass.newInstance();
				return true;
			} catch (Throwable e) {
				e.printStackTrace();
			}
			return false;
		}

        public InputStream loadResource(final String resPath) {
            try {
                if (_pluginPackage == null) {
                    return null;
                }
                final ZipEntry _entry = _pluginPackage.getEntry(resPath);
                if (_entry == null) {
                    return null;
                }
                InputStream proxyStream = new InputStream() {
                    InputStream _entryStream = _pluginPackage.getInputStream(_entry);

					@Override
					public int read() throws IOException {
						return _entryStream.read();
					}

					@Override
					public int available() throws IOException {
						return _entryStream.available();
					}

					@Override
					public void close() throws IOException {
						_entryStream.close();
					}

					@Override
					public void mark(int readlimit) {
						_entryStream.mark(readlimit);
					}

					@Override
					public boolean markSupported() {
						return _entryStream.markSupported();
					}

					@Override
					public int read(byte[] buffer) throws IOException {
						return _entryStream.read(buffer);
					}

					@Override
					public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
						return _entryStream.read(buffer, byteOffset, byteCount);
					}

					@Override
					public synchronized void reset() throws IOException {
						if (_entryStream != null) {
							try {
								_entryStream.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						_entryStream = _pluginPackage.getInputStream(_entry);
					}

					@Override
					public long skip(long byteCount) throws IOException {
						return _entryStream.skip(byteCount);
					}
				};
				return proxyStream;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	static HashMap<XulPlugin, XulPluginInfo> _plugins = new HashMap<>();
	static HashMap<String, XulPluginInfo> _pluginPathMap = new HashMap<>();
	protected static HashMap<String, XulPluginInfo> _pluginNameMap = new HashMap<>();

    public static XulPlugin loadPlugin(File path) {
        XulPluginInfo xulPluginInfo = new XulPluginInfo();
        if (!xulPluginInfo.init(path)) {
            return null;
        }
        _pluginPathMap.put(xulPluginInfo._path.getAbsolutePath(), xulPluginInfo);
        _plugins.put(xulPluginInfo._plugin, xulPluginInfo);
        _pluginNameMap.put(xulPluginInfo._plugin.getName(), xulPluginInfo);
        return xulPluginInfo._plugin;
    }

    public static XulPlugin findPluginByPath(File path) {
        XulPluginInfo xulPluginInfo = _pluginPathMap.get(path.getAbsolutePath());
        if (xulPluginInfo == null) {
            return null;
        }
        return xulPluginInfo._plugin;
    }

    public static XulPlugin findPluginByName(String name) {
        XulPluginInfo xulPluginInfo = _pluginNameMap.get(name);
        if (xulPluginInfo == null) {
            return null;
        }
        return xulPluginInfo._plugin;
    }

    public static InputStream loadPluginResource(Object plugin, String resPath) {
        XulPluginInfo xulPluginInfo = null;
        if (plugin instanceof CharSequence) {
            xulPluginInfo = _pluginPathMap.get(plugin);
            if (xulPluginInfo == null) {
                xulPluginInfo = _pluginNameMap.get(plugin);
            }
        } else {
            xulPluginInfo = _plugins.get(plugin);
        }
        if (xulPluginInfo == null) {
            return null;
        }
        return xulPluginInfo.loadResource(resPath);
    }

    public static void deletePlugin(String pluginName) {
        XulPluginInfo pluginInfo = _pluginNameMap.get(pluginName);
        if (pluginInfo != null) {
            if (pluginInfo._path != null) {
                _pluginPathMap.remove(pluginInfo._path.getAbsolutePath());
            }
            _pluginNameMap.remove(pluginInfo._plugin.getName());
            _plugins.remove(pluginInfo._plugin);
        }
    }

    public static void deletePlugin(File path) {
        if (path == null) {
            return;
        }
        XulPluginInfo pluginInfo = _pluginPathMap.get(path.getAbsolutePath());
        if (pluginInfo != null) {
            if (pluginInfo._path != null) {
                _pluginPathMap.remove(pluginInfo._path.getAbsolutePath());
            }
            _pluginNameMap.remove(pluginInfo._plugin.getName());
            _plugins.remove(pluginInfo._plugin);
        }
    }

    public static void deletePlugin(XulPlugin plugin) {
        if (plugin == null) {
            return;
        }
        XulPluginInfo pluginInfo = _plugins.get(plugin);
        if (pluginInfo != null) {
            if (pluginInfo._path != null) {
                _pluginPathMap.remove(pluginInfo._path.getAbsolutePath());
            }
            _pluginNameMap.remove(pluginInfo._plugin.getName());
            _plugins.remove(pluginInfo._plugin);
        }
    }

    public static List<XulPlugin> getAllPlugins() {
        Collection<XulPluginInfo> values = _pluginNameMap.values();
        List<XulPlugin> result = new ArrayList<>();
        for (XulPluginInfo value : values) {
            result.add(value._plugin);
        }
        return result;
    }

    public static File findPathByName(String name) {
        XulPluginInfo info = _pluginNameMap.get(name);
        if (info != null) {
            return info._path;
        }
        return null;
    }

    public static XulPlugin loadLocalPlugin(String clazzName) {
        ClassLoader classLoader = XulPluginManager.class.getClassLoader();
        try {
            Class<?> pluginClass = classLoader.loadClass(clazzName);
            XulPlugin plugin = (XulPlugin) pluginClass.newInstance();
            String name = plugin.getName();
            XulPluginInfo pluginInfo = new XulPluginInfo();
            pluginInfo._path = null;
            pluginInfo._classLoader = classLoader;
            pluginInfo._plugin = plugin;
            pluginInfo._pluginPackage = null;
            _pluginNameMap.put(name, pluginInfo);
            _pluginPathMap.put(clazzName, pluginInfo);
            _plugins.put(plugin, pluginInfo);
            return plugin;
        } catch (ClassNotFoundException e) {
//                e.printStackTrace();
        } catch (InstantiationException e) {
//                e.printStackTrace();
        } catch (IllegalAccessException e) {
//                e.printStackTrace();
        }
        return null;
    }

}
