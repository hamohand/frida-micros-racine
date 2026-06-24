import os
from app import create_app

app = create_app()

if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8082))
    print("=== ROUTES ENREGISTREES ===")
    for rule in app.url_map.iter_rules():
        print(f"{rule.endpoint}: {rule.rule}")
    print("===========================")
    app.run(debug=True, use_reloader=False, host='0.0.0.0', port=port)
