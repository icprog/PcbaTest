package com.ty.actionspcbatest;

public class SwitchLogo{
	
	static{
		 System.loadLibrary("twd_test_flag");
	}

	public SwitchLogo(){
	}

	native protected byte nativeGetLogoIndex();
	native protected int nativeSetLogoIndex(byte index);

	public byte getLogoIndex(){
		return nativeGetLogoIndex();
	}

	public int setLogoIndex(byte index){
		return nativeSetLogoIndex(index);
	}
}
