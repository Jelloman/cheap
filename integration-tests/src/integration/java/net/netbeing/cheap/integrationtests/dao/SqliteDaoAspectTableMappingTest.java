package net.netbeing.cheap.integrationtests.dao;

import net.netbeing.cheap.db.AspectTableMapping;
import net.netbeing.cheap.db.sqlite.SqliteAdapter;
import net.netbeing.cheap.db.sqlite.SqliteDao;
import net.netbeing.cheap.impl.basic.CheapFactory;
import net.netbeing.cheap.integrationtests.base.SqliteRestIntegrationTest;
import net.netbeing.cheap.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SQLite DAO with AspectTableMapping.
 * Tests two custom tables with different AspectTableMapping patterns.
 */
class SqliteDaoAspectTableMappingTest extends SqliteRestIntegrationTest
{
    private SqliteDao sqliteDao;
    private SqliteAdapter adapter;
    private CheapFactory factory;

    private AspectDef productAspectDef;
    private AspectDef categoryAspectDef;

    private AspectTableMapping productTableMapping;
    private AspectTableMapping categoryTableMapping;

    @BeforeEach
    @Override
    public void setUp()
    {
        super.setUp();

        factory = new CheapFactory();
        adapter = new SqliteAdapter(dataSource, factory);
        sqliteDao = new SqliteDao(adapter);

        // Create AspectDef for product table (entity_id, sku, name, price)
        Map<String, PropertyDef> productProps = new LinkedHashMap<>();
        productProps.put("sku", factory.createPropertyDef("sku", PropertyType.String));
        productProps.put("name", factory.createPropertyDef("name", PropertyType.String));
        productProps.put("price", factory.createPropertyDef("price", PropertyType.Float));
        productAspectDef = factory.createImmutableAspectDef("product", productProps);

        // Create AspectDef for category table (catalog_id, category_name, description)
        Map<String, PropertyDef> categoryProps = new LinkedHashMap<>();
        categoryProps.put("category_name", factory.createPropertyDef("category_name", PropertyType.String));
        categoryProps.put("description", factory.createPropertyDef("description", PropertyType.String));
        categoryAspectDef = factory.createImmutableAspectDef("category", categoryProps);

        // Create AspectTableMapping for product (Pattern 3: Entity ID only)
        Map<String, String> productColumnMapping = Map.of(
            "sku", "sku",
            "name", "name",
            "price", "price"
        );
        productTableMapping = new AspectTableMapping(
            productAspectDef,
            "product",
            productColumnMapping,
            false,  // hasCatalogId
            true    // hasEntityId
        );

        // Create AspectTableMapping for category (Pattern 2: Catalog ID only)
        Map<String, String> categoryColumnMapping = Map.of(
            "category_name", "category_name",
            "description", "description"
        );
        categoryTableMapping = new AspectTableMapping(
            categoryAspectDef,
            "category",
            categoryColumnMapping,
            true,   // hasCatalogId
            false   // hasEntityId
        );

        // Register mappings with DAO
        sqliteDao.addAspectTableMapping(productTableMapping);
        sqliteDao.addAspectTableMapping(categoryTableMapping);

        // Create the custom tables
        try
        {
            sqliteDao.createTable(productTableMapping);
            sqliteDao.createTable(categoryTableMapping);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to create custom tables", e);
        }
    }

    @Test
    void testSaveAndLoadCatalogWithCustomTables() throws Exception
    {
        // Create catalog
        UUID catalogId = testUuid(1000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        // Create AspectMapHierarchy for product aspects
        AspectMapHierarchy productHierarchy = factory.createAspectMapHierarchy(catalog, productAspectDef);

        // Add product data
        UUID productId1 = testUuid(1);
        Entity product1 = factory.createEntity(productId1);
        Aspect productAspect1 = factory.createPropertyMapAspect(product1, productAspectDef);
        productAspect1.put(factory.createProperty(productAspectDef.propertyDef("sku"), "SKU-001"));
        productAspect1.put(factory.createProperty(productAspectDef.propertyDef("name"), "Widget"));
        productAspect1.put(factory.createProperty(productAspectDef.propertyDef("price"), 19.99));
        productHierarchy.put(product1, productAspect1);

        UUID productId2 = testUuid(2);
        Entity product2 = factory.createEntity(productId2);
        Aspect productAspect2 = factory.createPropertyMapAspect(product2, productAspectDef);
        productAspect2.put(factory.createProperty(productAspectDef.propertyDef("sku"), "SKU-002"));
        productAspect2.put(factory.createProperty(productAspectDef.propertyDef("name"), "Gadget"));
        productAspect2.put(factory.createProperty(productAspectDef.propertyDef("price"), 29.99));
        productHierarchy.put(product2, productAspect2);

        // Create AspectMapHierarchy for category aspects
        AspectMapHierarchy categoryHierarchy = factory.createAspectMapHierarchy(catalog, categoryAspectDef);

        // Add category data (entity IDs will be generated since no entity_id column)
        Entity category1 = factory.createEntity();
        Aspect categoryAspect1 = factory.createPropertyMapAspect(category1, categoryAspectDef);
        categoryAspect1.put(factory.createProperty(categoryAspectDef.propertyDef("category_name"), "Electronics"));
        categoryAspect1.put(factory.createProperty(categoryAspectDef.propertyDef("description"), "Electronic devices and accessories"));
        categoryHierarchy.put(category1, categoryAspect1);

        // Save catalog
        sqliteDao.saveCatalog(catalog);

        // Verify product data in custom table
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT entity_id, sku, name, price FROM product ORDER BY sku"))
        {
            assertTrue(rs.next());
            assertEquals(productId1.toString(), rs.getString("entity_id"));
            assertEquals("SKU-001", rs.getString("sku"));
            assertEquals("Widget", rs.getString("name"));
            assertEquals(19.99, rs.getDouble("price"), 0.01);

            assertTrue(rs.next());
            assertEquals(productId2.toString(), rs.getString("entity_id"));
            assertEquals("SKU-002", rs.getString("sku"));
            assertEquals("Gadget", rs.getString("name"));
            assertEquals(29.99, rs.getDouble("price"), 0.01);

            assertFalse(rs.next());
        }

        // Verify category data in custom table
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT catalog_id, category_name, description FROM category"))
        {
            assertTrue(rs.next());
            assertEquals(catalogId.toString(), rs.getString("catalog_id"));
            assertEquals("Electronics", rs.getString("category_name"));
            assertEquals("Electronic devices and accessories", rs.getString("description"));

            assertFalse(rs.next());
        }

        // Load catalog back
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        assertNotNull(loadedCatalog);

        // Verify product aspects loaded correctly
        AspectMapHierarchy loadedProductHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        assertNotNull(loadedProductHierarchy);
        assertEquals(2, loadedProductHierarchy.size());

        Aspect loadedProduct1 = loadedProductHierarchy.get(product1);
        assertNotNull(loadedProduct1);
        assertEquals("SKU-001", loadedProduct1.readObj("sku"));
        assertEquals("Widget", loadedProduct1.readObj("name"));
        assertEquals(19.99, loadedProduct1.readObj("price"));

        Aspect loadedProduct2 = loadedProductHierarchy.get(product2);
        assertNotNull(loadedProduct2);
        assertEquals("SKU-002", loadedProduct2.readObj("sku"));
        assertEquals("Gadget", loadedProduct2.readObj("name"));
        assertEquals(29.99, loadedProduct2.readObj("price"));

        // Verify category aspects loaded correctly
        AspectMapHierarchy loadedCategoryHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("category");
        assertNotNull(loadedCategoryHierarchy);
        assertEquals(1, loadedCategoryHierarchy.size());

        // Category entity ID will be different (generated on load)
        Entity loadedCategory = loadedCategoryHierarchy.keySet().iterator().next();
        Aspect loadedCategoryAspect = loadedCategoryHierarchy.get(loadedCategory);
        assertNotNull(loadedCategoryAspect);
        assertEquals("Electronics", loadedCategoryAspect.readObj("category_name"));
        assertEquals("Electronic devices and accessories", loadedCategoryAspect.readObj("description"));
    }

    @Test
    void testUpdateAspectsInCustomTables() throws Exception
    {
        // Create and save initial catalog
        UUID catalogId = testUuid(2000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy productHierarchy = factory.createAspectMapHierarchy(catalog, productAspectDef);
        UUID productId = testUuid(10);
        Entity product = factory.createEntity(productId);
        Aspect productAspect = factory.createPropertyMapAspect(product, productAspectDef);
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("sku"), "SKU-100"));
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("name"), "Original Name"));
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("price"), 99.99));
        productHierarchy.put(product, productAspect);

        sqliteDao.saveCatalog(catalog);

        // Update the aspect
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("name"), "Updated Name"));
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("price"), 89.99));
        sqliteDao.saveCatalog(catalog);

        // Verify update in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT sku, name, price FROM product WHERE entity_id = '" + productId + "'"))
        {
            assertTrue(rs.next());
            assertEquals("SKU-100", rs.getString("sku"));
            assertEquals("Updated Name", rs.getString("name"));
            assertEquals(89.99, rs.getDouble("price"), 0.01);
        }

        // Load and verify
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        AspectMapHierarchy loadedHierarchy = (AspectMapHierarchy) loadedCatalog.hierarchy("product");
        Aspect loadedAspect = loadedHierarchy.get(product);
        assertEquals("Updated Name", loadedAspect.readObj("name"));
        assertEquals(89.99, loadedAspect.readObj("price"));
    }

    @Test
    void testDeleteCatalogCleansUpCustomTables() throws Exception
    {
        // Create and save catalog with data
        UUID catalogId = testUuid(3000);
        Catalog catalog = factory.createCatalog(catalogId, CatalogSpecies.SINK, null, null, 0L);

        AspectMapHierarchy productHierarchy = factory.createAspectMapHierarchy(catalog, productAspectDef);
        UUID productId = testUuid(20);
        Entity product = factory.createEntity(productId);
        Aspect productAspect = factory.createPropertyMapAspect(product, productAspectDef);
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("sku"), "SKU-TEST"));
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("name"), "Test Product"));
        productAspect.put(factory.createProperty(productAspectDef.propertyDef("price"), 50.00));
        productHierarchy.put(product, productAspect);

        AspectMapHierarchy categoryHierarchy = factory.createAspectMapHierarchy(catalog, categoryAspectDef);
        Entity category = factory.createEntity();
        Aspect categoryAspect = factory.createPropertyMapAspect(category, categoryAspectDef);
        categoryAspect.put(factory.createProperty(categoryAspectDef.propertyDef("category_name"), "Test"));
        categoryAspect.put(factory.createProperty(categoryAspectDef.propertyDef("description"), "Test category"));
        categoryHierarchy.put(category, categoryAspect);

        sqliteDao.saveCatalog(catalog);

        // Verify data exists
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM product"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM category WHERE catalog_id = '" + catalogId + "'"))
        {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }

        // Delete catalog
        sqliteDao.deleteCatalog(catalogId);

        // Verify product table is empty (no catalog_id to filter by)
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM product"))
        {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify category table has no rows for this catalog
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM category WHERE catalog_id = '" + catalogId + "'"))
        {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }

        // Verify catalog cannot be loaded
        Catalog loadedCatalog = sqliteDao.loadCatalog(catalogId);
        assertNull(loadedCatalog);
    }

    @Override
    protected void cleanupDatabase() throws Exception
    {
        super.cleanupDatabase();

        // Also clean up custom tables
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute("DROP TABLE IF EXISTS product");
            stmt.execute("DROP TABLE IF EXISTS category");
        }
        catch (Exception e)
        {
            // Ignore errors during cleanup
        }
    }
}
