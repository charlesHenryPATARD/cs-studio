"""
NumJy - NumPy-inspired array support for Jython
-----------------------------------------------

See also 
  help(ndarray)
"""

# Path setup
import sys, os

try:
    # If this succeeds, the binaries are already in the classpath
    # (set on command line, or running within scan server)
    import org.csstudio.ndarray.NDArray as NDArray
except:
    # Need to locate NDArray binaries
    try:
        # When running under Eclipse, locate NDArray bundle
        from org.eclipse.core.runtime import Platform
        from org.eclipse.core.runtime import FileLocator
        from org.eclipse.core.runtime import Path
        bundle = Platform.getBundle("org.csstudio.ndarray")
        url = FileLocator.find(bundle, Path("lib/util.jar"), None)
        if not url:
            raise Exception("Missing org.csstudio.ndarray/lib/util.jar")
        sys.path.append(FileLocator.resolve(url).getPath())
        # While in the IDE, the plugin classes are in a bin subdir
        url = FileLocator.find(bundle, Path("bin"), None)
        if url:
            sys.path.append(FileLocator.resolve(url).getPath())
        else:
            # In an exported product, the classes are at the root
            sys.path.append(FileLocator.getBundleFile(bundle).getPath())
    except:
        # Not running under Eclipse
        # When executing tests from IDE/repository,
        # use binaries compiled by IDE
        if os.path.exists("../../org.csstudio.ndarray/bin"):
            sys.path.append("../../org.csstudio.ndarray/bin")
            sys.path.append("../../org.csstudio.ndarray/lib/util.jar")
        else:
            # Add other method of locating ndarray binaries?
            raise Exception("NumJy library bineries not configured")


version_info = (0, 1)
version = "NumJy V%d.%d" % version_info

if os.name == 'java':
    # Import NumJy
    from ndarray import *
else:
    # Fall back to original numpy
    from numpy import *
    
