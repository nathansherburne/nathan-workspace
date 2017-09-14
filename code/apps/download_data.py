#!/usr/bin/env python2.7
import sys, getopt
import os
import pdfToCSVlib as p2c
import argparse
import download_lib as dw
from argparse_lib import is_dir


class MyAction(argparse.Action):
    def __call__(self, parser, namespace, values, option_string=None):
        setattr(namespace, self.dest, values)
        print namespace

def main(argv):
        os.environ["ENVTEST"] = "1"
        parser = argparse.ArgumentParser()
        parser.add_argument("-o", "--output", metavar="dir", type=is_dir, help="the output directory")
        parser.add_argument("-d", "--dataset", type=str, required=True, choices=['MX', 'TW', 'PE', 'Rio', 'PAHO', 'LK', 'TN', 'CME'], help="the data set to download")
        parser.add_argument("-u", "--update", action='store_true', default=False)
        args = parser.parse_args()
        
        if args.output:
            out_dir = args.output
        else:
            out_dir = os.getcwd()
        
        if args.dataset == 'MX':
            dw.getMexicoData(out_dir, args.update)
        if args.dataset == 'TW':
            dw.getTaiwanData(out_dir, args.update)
        if args.dataset == 'PE':
            dw.getPeruData(out_dir, args.update)
        if args.dataset == 'Rio':
            dw.getRioData(out_dir, args.update)
        if args.dataset == 'PAHO':
            dw.getPAHOData(out_dir, args.update)
        if args.dataset == 'LK':
            dw.getSriLankaData(out_dir, args.update)
        if args.dataset == 'TN':
            dw.getTennesseeData(out_dir, args.update)
        if args.dataset == 'CME':
            dw.getCMEScoreboards(out_dir, args.update)


if __name__ == "__main__":
    main(sys.argv[1:])
