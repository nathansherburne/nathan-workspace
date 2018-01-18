Converting: denguelib/java/PDF2CSV
----------------
Libraries: PDFBox

```
usage: java -jar PDF2CSV.jar
 -i,--input <arg>    input file path
  -o,--output <arg>   output file

   Example   : java -jar PDF2CSV.jar -i rio/data/PDFs/weekly/* -o rio/data/CSVs/individual/weekly/
   ```

   Similar to pdfminer, but more low level. Each page of a PDF is processed by the engine, which can extract the points which make up rectangles. These points are used to create vertical and horizonal lines. The lines are compared, and every intersection between each pair of vertical and horizontal lines are used to generate more points. During the process, the relationship between points is maintained by defining "neighboors" for each point. Each point can have a nieghboor above, below, left, and right of it. These relationships are important because, after all the points are generated into a grid-like structure, we can then use the neighboor relationships to define meaningful rectangles. Once these rectangles are generated, they can be used to extract the text that they contain(like in the pdfminer "By Cell Boundaries" example).

   It has only been successfully tested on Rio's weekly PDFs. Much, much more work is required for this to be anywhere close to a general solution.
