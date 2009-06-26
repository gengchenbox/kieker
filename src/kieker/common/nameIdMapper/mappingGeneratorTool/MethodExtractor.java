package kieker.common.nameIdMapper.mappingGeneratorTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This Program extracts all method signatures from a given input path in
 * classpath style. 
 * 
 * @author Robert von Massow
 * 
 */
public class MethodExtractor extends ClassLoader {

	public static void main(final String[] args) throws ClassNotFoundException {
		if (args.length == 0) {
			printUsage();
			return;
		}
		MethodExtractor foo = new MethodExtractor();
		Hashtable<String, String> argT = new Hashtable<String, String>();
		try {
			parseArgs(args, argT);
		} catch (Exception e) {
			printUsage();
		}
		foo.analyse(argT);
	}

	private static void printUsage() {
		System.err.println("Usage: java MethodExtractor [-f filter] path");
	}

	private static void parseArgs(final String[] args,
			final Hashtable<String, String> argT) {
		String key = "cp";
		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				String option = args[i].substring(1);
				if (option.equals("f")) {
					key = "filter";
				}
			} else {
				argT.put(key, args[i]);
				key = "cp";
			}
		}

	}

	@SuppressWarnings("unchecked")
	private void analyse(final Hashtable<String, String> argT)
			throws ClassNotFoundException {
		File out = new File("current.map");
		try {
			System.setOut(new PrintStream(new FileOutputStream(out)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String[] elem = argT.get("cp").split(File.pathSeparator);
		Class<MethodFilter> filter = null;
		try {
			filter = (Class<MethodFilter>) loadClass(argT.get("filter"));
		} catch (Exception e) {
			System.err.println("Unable to load filter or no filter provided ("
					+ argT.get("filter") + "), using default filter...");
			e.printStackTrace();
		}
		Vector<File> directories = new Vector<File>();
		Vector<File> jars = new Vector<File>();
		for (String string : elem) {
			File f = new File(string);
			if (f.isDirectory()) {
				directories.add(f);
			} else if (f.getName().endsWith(".jar")) {
				jars.add(f);
			}
		}
		analyzeDirectory(directories, filter);
		analyzeJars(jars, filter);
	}

	private void analyzeJars(final Vector<File> jars,
			final Class<MethodFilter> filter) {
		MethodFilter f = null;
		if (filter != null) {
			try {
				f = filter.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		if (f == null) {
			f = new NullFilter();
		}
		for (File file : jars) {
			try {
				JarFile jar = new JarFile(file.getAbsoluteFile().getName());
				analyseJarFile(jar, f);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void analyseJarFile(final JarFile jar, final MethodFilter filter) {
		Enumeration<JarEntry> es = jar.entries();
		while (es.hasMoreElements()) {
			JarEntry e = es.nextElement();
			System.err.println(e.getName());
			if (e.getName().endsWith(".class")) {
				analyzeJarClassEntry(jar, e, "", filter);
			}
		}
	}

	private void analyzeJarClassEntry(final JarFile jar, final JarEntry e,
			final String prefix, final MethodFilter filter) {
		byte[] data = null;
		long size = e.getSize();
		if (size == -1 || size > Integer.MAX_VALUE) {
			System.err.println("Size of file \"" + jar.getName() + "/"
					+ e.getName() + " out of range: size");
			return;
		}
		data = new byte[(int) size];
		try {
			jar.getInputStream(e).read(data);
			String name = e.getName().substring(0, e.getName().length() - 6)
					.replaceAll("/", ".");
			Class<?> clazz = null;
			try {
				clazz = defineClass(name, data, 0, data.length);
				analyzeClass(clazz, filter);
			} catch (LinkageError err) {
				System.err.println(err.getMessage());
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void analyzeDirectory(final Vector<File> dirs,
			final Class<MethodFilter> filter) {
		MethodFilter f = null;
		if (filter != null) {
			try {
				f = filter.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			if (f == null) {
				f = new NullFilter();
			}
		}
		for (File dir : dirs) {
			descendAndAnalyseDir(dir, "", f);
		}
	}

	/**
	 * recursively descent the "classpath" and analyze the classes that a found
	 * in it.
	 * 
	 * @param dir
	 *            current system directory
	 * @param packagePrefix
	 *            the packageprefix
	 */
	private void descendAndAnalyseDir(final File dir,
			final String packagePrefix, final MethodFilter filter) {
		File[] classes = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File dir, final String name) {
				return name.endsWith(".class");
			}

		});
		File[] descArray = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(final File f, final String name) {
				return new File(f, name).isDirectory();
			}
		});
		analyseClasses(classes, packagePrefix, filter);
		classes = null;
		for (File file : descArray) {
			descendAndAnalyseDir(file, (packagePrefix + file.getName()
					.replaceFirst(dir.getName(), "")).replaceAll(
					File.separator, "."), filter);
		}
	}

	/**
	 * extract all methods from the given classes
	 * 
	 * @param classes
	 *            the classes in the current package
	 * @param packagePrefix
	 *            the current package
	 */
	private void analyseClasses(final File[] classes,
			final String packagePrefix, final MethodFilter filter) {
		for (File file : classes) {
			int begIndex = file.getName().lastIndexOf(File.separatorChar);
			begIndex = begIndex == -1 ? 0 : begIndex;
			int endIndex = file.getName().lastIndexOf('.');
			System.out.println("analyzing " + packagePrefix
					+ file.getName().substring(begIndex, endIndex));
			try {
				Class<?> c = super.loadClass(packagePrefix
						+ file.getName().substring(begIndex, endIndex));
				analyzeClass(c, filter);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void analyzeClass(final Class<?> c, final MethodFilter filter) {
		Method[] m = c.getDeclaredMethods();
		for (Method method : m) {
			if (!filter.accept(method, c)) {
				continue;
			}
			int mod = method.getModifiers();
			String prefix = "";
			if (Modifier.isPublic(mod)) {
				prefix += "public ";
			}
			if (Modifier.isPrivate(mod)) {
				prefix += "private ";
			}
			if (Modifier.isProtected(mod)) {
				prefix += "protected ";
			}
			if (Modifier.isNative(mod)) {
				prefix += "native ";
			}
			if (Modifier.isAbstract(mod)) {
				prefix += "abstract ";
			}
			if (Modifier.isStatic(mod)) {
				prefix += "static ";
			}
			if (Modifier.isFinal(mod)) {
				prefix += "final ";
			}
			System.out.println("found method " + prefix
					+ method.getDeclaringClass().getName() + "."
					+ method.getName() + "("
					+ concat(method.getParameterTypes()) + ")");
			method.getAnnotation(Override.class);
		}
	}

	/**
	 * format a parameter list
	 * 
	 * @param parameterTypes
	 *            the method parameters
	 * @return formated parameter list
	 */
	private String concat(final Class<?>[] parameterTypes) {
		StringBuilder ret = new StringBuilder();
		for (Class<?> class1 : parameterTypes) {
			ret.append(class1.getCanonicalName() + ", ");
		}
		if (ret.length() == 0) {
			return "";
		}
		return ret.delete(ret.length() - 2, ret.length()).toString();
	}

}

class NullFilter implements MethodFilter {

	@Override
	public boolean accept(final Method m, final Class<?> c) {
		return true;
	}

}
