package extraction;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.TextStripper;
import technology.tabula.Utils;

public class MyObjectExtractor extends ObjectExtractor {

	private PDDocument pdfDocument;

	public MyObjectExtractor(PDDocument pdfDocument) throws IOException {
		super(pdfDocument);
		this.pdfDocument = pdfDocument;
	}

	protected Page extractPage(Integer pageNumber) throws IOException {
		if (pageNumber > this.pdfDocument.getNumberOfPages() || pageNumber < 1) {
            throw new java.lang.IndexOutOfBoundsException(
                    "Page number does not exist");
        }

        PDPage p = this.pdfDocument.getPage(pageNumber - 1);

        RulingExtractor re = new RulingExtractor(pdfDocument);
        re.processPage(pageNumber-1);

        TextStripper pdfTextStripper = new TextStripper(this.pdfDocument, pageNumber);
        pdfTextStripper.process();
        Utils.sort(pdfTextStripper.textElements);

        float w, h;
        int pageRotation = p.getRotation();
        if (Math.abs(pageRotation) == 90 || Math.abs(pageRotation) == 270) {
            w = p.getCropBox().getHeight();
            h = p.getCropBox().getWidth();
        } else {
            w = p.getCropBox().getWidth();
            h = p.getCropBox().getHeight();
        }

        return new Page(0, 0, w, h, pageRotation, pageNumber, p, pdfTextStripper.textElements,
                re.rulings, pdfTextStripper.minCharWidth, pdfTextStripper.minCharHeight, pdfTextStripper.spatialIndex);
    }
	

    public PageIterator extract(Iterable<Integer> pages) {
        return new PageIterator(this, pages);
    }

    public PageIterator extract() {
        return extract(Utils.range(1, this.pdfDocument.getNumberOfPages() + 1));
    }

    public Page extract(int pageNumber) {
        return extract(Utils.range(pageNumber, pageNumber + 1)).next();
    }

    public void close() throws IOException {
        this.pdfDocument.close();
    }
}
