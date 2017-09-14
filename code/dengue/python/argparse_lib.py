import os
import argparse

def is_dir(dirname):
    """Checks if a path is an actual directory"""
    if not os.path.isdir(dirname):
        msg = "{0} is not a directory".format(dirname)
        raise argparse.ArgumentTypeError(msg)
    else:
        return os.path.abspath(dirname)

def is_html(filename):
    if not '.htm' in os.path.splitext(filename)[1]:
        msg = "{} is not an HTML file".format(filename)
        raise argparse.ArgumentTypeError(msg)
    else:
        return os.path.abspath(filename)

def is_pdf(filename):
    if not '.pdf' in os.path.splitext(filename)[1]:
        msg = "{} is not a PDF file".format(filename)
        raise argparse.ArgumentTypeError(msg)
    else:
        return os.path.abspath(filename)

def attr_pair(arg):
    if len(arg.split(':')) is not 2:
        msg = "{}: Pair must be in format: <attribute>:<value>".format(arg)
        raise argparse.ArgumentTypeError(msg)
    else:
        return arg.split(':')
