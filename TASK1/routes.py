from django import db
from flask import app, request, jsonify
from sqlalchemy.exc import IntegrityError  # type: ignore
from decimal import Decimal, InvalidOperation

"""
=========================================================================================================================================
--------------------------------------------------
     ISSUES FOUND IN THE GIVEN PYTHON CODE
--------------------------------------------------

1."request.json" may be None:
     -If client sends invalid JSON or no JSON, request.json is None.
     -Accessing data['name'] will raise TypeError.

2.No response status code:    
     -Flask default returns 200 OK. For resource creation, correct response should be 201 Created.

3.No error handling done at all:
     -If DB fails, API will throw a 500 instead of returning a structured JSON error.

4.Committing twice (db.session.commit()):
     -First commit saves product → then second commit saves inventory.
     -If second fails, you end up with an orphan product without inventory (inconsistent state).

5.No type casting for fields:
     -In "price=data['price']" could be string from JSON 
     -DB insert may fail if column expects Decimal or Float.
     -in "quantity=data['initial_quantity']" same risk if client sends string.

6.Assumes that all fields exist:
     -If initial_quantity is missing, will raise KeyError.
     -Same with warehouse_id (if product isn’t tied to a warehouse).

7.Returning plain dict instead of Flask jsonify():
     -Flask will auto-convert dict to JSON, but best practice is return jsonify(...), status_code.

8.No validation for negative values:
     -Price and quantity can be negative that is invalid business logic.

9.No uniqueness check for SKU in code
     -If two products try to use the same SKU, the API breaks instead of telling the user “This SKU already exists.”

=========================================================================================================================================
"""


@app.route("/api/products", methods=["POST"])
def create_product():
    # Safely parse JSON body (if no JSON, use empty dict instead of crashing)
    data = request.get_json() or {}

    # --- Validate required fields ---
    # Ensures that "name", "sku", and "price" are always provided by the client
    required_fields = ["name", "sku", "price"]
    missing = [f for f in required_fields if f not in data]
    if missing:
        return jsonify({"error": f"Missing required fields: {', '.join(missing)}"}), 422

    # --- Validate price ---
    try:
        # Convert price to Decimal for precision (avoids float rounding issues)
        price = Decimal(str(data["price"]))
        if price < 0:
            # Price cannot be negative
            return jsonify({"error": "Price must be non-negative"}), 422
    except (InvalidOperation, TypeError):
        # Handle invalid values like "abc" or wrong data types
        return jsonify({"error": "Invalid price format"}), 422

    # --- Optional fields (validate if present) ---
    warehouse_id = data.get("warehouse_id")
    # Default quantity is 0 when omitted; must be a non-negative integer.
    initial_quantity = data.get("initial_quantity", 0)
    #  using try-except for proper error handling
    try:
        # --- Transaction block ---
        # Creating product object
        product = Product(name=data["name"], sku=data["sku"], price=price)
        db.session.add(product)  # Add to session
        db.session.flush()  # Assigns product.id without committing (keeps atomicity)

        # If warehouse info provided, also create inventory entry
        if warehouse_id:
            inventory = Inventory(
                product_id=product.id,
                warehouse_id=warehouse_id,
                quantity=initial_quantity,
            )
            db.session.add(inventory)

        # Commit once for both product and inventory
        db.session.commit()

        # Success response with correct HTTP status 201 Created
        return jsonify({"message": "Product created", "product_id": product.id}), 201

    except IntegrityError as e:
        # Likely caused by duplicate SKU (unique constraint violation)
        db.session.rollback()  # Undo transaction
        return jsonify({"error": "SKU already exists"}), 400

    except Exception as e:
        # Catch-all for unexpected errors (DB down, invalid types, etc.)
        db.session.rollback()  # Undo partial changes
        return jsonify({"error": str(e)}), 500
