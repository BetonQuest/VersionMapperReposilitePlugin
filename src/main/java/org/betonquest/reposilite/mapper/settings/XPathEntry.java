package org.betonquest.reposilite.mapper.settings;

import com.reposilite.configuration.shared.api.Doc;
import com.reposilite.configuration.shared.api.Min;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

/**
 * Represents an xpath entry in the settings.
 *
 * @param id    The ID of the entry
 * @param xpath The xpath value
 */
@SuppressWarnings("PMD.ShortVariable")
@Doc(title = "XPathEntry", description = "A list of all xPaths extracting string from the pom")
public record XPathEntry(
        @Min(min = 1) @Doc(title = "id", description = "The ID of the entry")
        String id,
        @Doc(title = "xpath", description = "The xpath value")
        String xpath) {

    /**
     * Parses the given document and returns the value of the xpath.
     *
     * @param document The document to parse.
     * @param xPath    The xpath to evaluate.
     * @return The value of the xpath.
     * @throws XPathExpressionException If the xpath could not be evaluated.
     */
    public String parse(final XPath xPath, final Document document) throws XPathExpressionException {
        final XPathExpression xPathExpression = xPath.compile(xpath());
        return (String) xPathExpression.evaluate(document, XPathConstants.STRING);
    }
}
