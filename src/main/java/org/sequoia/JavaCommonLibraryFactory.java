package org.sequoia;

import org.sequoia.implementation.JavaCommonLibraryImpl;

public class JavaCommonLibraryFactory
{
	public static JavaCommonLibrary createInstance()
	{
		return new JavaCommonLibraryImpl();
	}
}
