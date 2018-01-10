#!/usr/local/bin/
import sys, getopt
import os
import argparse
#import download_lib as dw
import mexico_dwn
import rio_dwn
import tennessee_dwn
import san_diego_dwn
import sri_lanka_dwn
import paho_dwn
import taiwan_dwn
import cme_scoreboards_dwn
import who_dwn
from argparse_lib import is_dir


class MyAction(argparse.Action):
    def __call__(self, parser, namespace, values, option_string=None):
        setattr(namespace, self.dest, values)
        print namespace

def main(argv):
        os.environ["ENVTEST"] = "1"
        parser = argparse.ArgumentParser()
        parser.add_argument("-o", "--output", metavar="dir", type=is_dir, help="the output directory")
        parser.add_argument("-d", "--dataset", type=str, required=True, choices=['SD', 'WHO', 'MX', 'TW', 'PE', 'RIO', 'PAHO', 'LK', 'TN', 'CME'], help="the data set to download")
        parser.add_argument("-u", "--update", action='store_true', default=False, help="Downloads only the current data")
        args = parser.parse_args()

        if args.output:
            out_dir = args.output
        else:
            out_dir = os.getcwd()

        if args.dataset == "SD":
            san_diego_dwn.getSanDiegoData(out_dir, args.update)
        if args.dataset == 'WHO':
            who_dwn.getWHOData(out_dir, args.update)
        if args.dataset == 'MX':
            mexico_dwn.getMexicoData(out_dir, args.update)
        if args.dataset == 'TW':
            taiwan_dwn.getTaiwanData(out_dir, args.update)
#        if args.dataset == 'PE':
#            dw.getPeruData(out_dir, args.update)
        if args.dataset == 'RIO':
            rio_dwn.getRioData(out_dir, args.update)
        if args.dataset == 'PAHO':
            paho_dwn.getPAHOData(out_dir, args.update)
        if args.dataset == 'LK':
            sri_lanka_dwn.getSriLankaData(out_dir, args.update)
        if args.dataset == 'TN':
            tennessee_dwn.getTennesseeData(out_dir, args.update)
        if args.dataset == 'CME':
            cme_scoreboards_dwn.getCMEScoreboards(out_dir, args.update)


if __name__ == "__main__":
    main(sys.argv[1:])
