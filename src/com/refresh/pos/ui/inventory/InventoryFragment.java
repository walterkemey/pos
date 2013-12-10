package com.refresh.pos.ui.inventory;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentIntegratorSupportV4;
import com.google.zxing.integration.android.IntentResult;
import com.refresh.pos.R;
import com.refresh.pos.domain.inventory.Inventory;
import com.refresh.pos.domain.inventory.Product;
import com.refresh.pos.domain.inventory.ProductCatalog;
import com.refresh.pos.domain.sale.Register;
import com.refresh.pos.domain.sale.SaleLedger;
import com.refresh.pos.techicalservices.NoDaoSetException;
import com.refresh.pos.ui.MainActivity;
import com.refresh.pos.ui.component.ButtonAdapter;
import com.refresh.pos.ui.component.UpdatableFragment;

@SuppressLint("ValidFragment")
public class InventoryFragment extends UpdatableFragment {

	protected static final int SEARCH_LIMIT = 0;
	private ListView inventoryListView;
	private ProductCatalog productCatalog;
	List<Map<String, String>> inventoryList;
	private Button addProductButton;
	private EditText searchBox;
	private Button scanButton;
	
	private ViewPager viewPager;
	private Register register;
	private AlertDialog.Builder popDialog;
	private LayoutInflater inflaters;
	private MainActivity main;
	
	private UpdatableFragment saleFragment;
	
	public InventoryFragment(UpdatableFragment saleFragment) {
		super();
		this.saleFragment = saleFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		try {
			productCatalog = Inventory.getInstance().getProductCatalog();
			register = Register.getInstance();
		} catch (NoDaoSetException e) {
			e.printStackTrace();
		}
		
		View view = inflater.inflate(R.layout.layout_inventory, container, false);
		popDialog = new AlertDialog.Builder(this.getActivity().getBaseContext());
		inflaters = (LayoutInflater) this.getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		
		inventoryListView = (ListView) view.findViewById(R.id.inventoryListView);
		addProductButton = (Button) view.findViewById(R.id.addProductButton);
		scanButton = (Button) view.findViewById(R.id.scanButton);
		searchBox = (EditText) view.findViewById(R.id.searchBox);
		
		main = (MainActivity) getActivity();
		viewPager = main.getViewPager();
		
		initUI();
		return view;
	}
	
	private void initUI() {
		
		addProductButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showPopup(v);
			}
		});
		
		searchBox.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	if (s.length() >= SEARCH_LIMIT) {
	        		search();
	        	}
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    });
		
		inventoryListView.setOnItemClickListener(new OnItemClickListener() {
		      public void onItemClick(AdapterView<?> myAdapter, View myView, int position, long mylng) {
		    	  int id = Integer.parseInt(inventoryList.get(position).get("id").toString());
		    	  register.addItem(productCatalog.getProductById(id), 1);
		    	  saleFragment.update();
		    	  viewPager.setCurrentItem(1);
		      }     
		});

		scanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				IntentIntegratorSupportV4 scanIntegrator = new IntentIntegratorSupportV4(InventoryFragment.this);
				scanIntegrator.initiateScan();
			}
		});
		
	}

	private void showList(List<Product> list) {
		
		inventoryList = new ArrayList<Map<String, String>>();
		for(Product product : list) {
			inventoryList.add(product.toMap());
		}
		
		ButtonAdapter sAdap = new ButtonAdapter(getActivity().getBaseContext(), inventoryList,
				R.layout.listview_inventory, new String[]{"name"}, new int[] {R.id.name}, R.id.optionView, "id");
		inventoryListView.setAdapter(sAdap);
	}
	
	private void search() {
		String search = searchBox.getText().toString();
		
		//TODO: DELTE THIS
		if (search.equals("motherlode")) {
			searchBox.setText("");
			testAddProduct();
		} else if (search.equals("clear")) {
			
			try {
				Inventory.getInstance().getProductCatalog().clearProductCatalog();
				Inventory.getInstance().getStock().clearStock();
				SaleLedger.getInstance().clearSaleLedger();
			} catch (NoDaoSetException e) {
				e.printStackTrace();
			}
		}
		
		if (search.equals("")) {
			showList(productCatalog.getAllProduct());
		} else {
			List<Product> result = productCatalog.searchProduct(search);
			showList(result);
			if (result.isEmpty()) {
				Toast.makeText(getActivity().getBaseContext() , "No results matched.", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.d("BARCODE", "retrive the result.");

		IntentResult scanningResult = IntentIntegrator.parseActivityResult(
				requestCode, resultCode, intent);

		if (scanningResult != null) {
			String scanContent = scanningResult.getContents();
			searchBox.setText(scanContent);
		} else {
			Toast.makeText(getActivity().getBaseContext() ,
					"Failed to retrieve barcode." + resultCode,
					Toast.LENGTH_SHORT).show();
		}
	}
	
	protected void testAddProduct() {
		try {
			
			Inventory.getInstance().getProductCatalog().addProduct("Apple iPhone 4s", "65005", 400 );
			Inventory.getInstance().getProductCatalog().addProduct("Apple Macbook Air (mid-2012)", "68701", 329 );
			Inventory.getInstance().getProductCatalog().addProduct("Television ", "20004", 200);
			Inventory.getInstance().getProductCatalog().addProduct("Lettuce" , "80775", 10.75);
			Inventory.getInstance().getProductCatalog().addProduct("Carrot", "10089", 28.50);
			Inventory.getInstance().getProductCatalog().addProduct("Sumsung Television", "899089", 48.50);
			Inventory.getInstance().getProductCatalog().addProduct("Applying UML and Pattern", "05667", 1.50);
			Inventory.getInstance().getProductCatalog().addProduct("Code Complete 2nd Edition", "99887", 2.50);
			Inventory.getInstance().getProductCatalog().addProduct("Banana", "9822337", 32.50);
			Inventory.getInstance().getProductCatalog().addProduct("Egg", "03011", 0.50);
			Inventory.getInstance().getProductCatalog().addProduct("Tomato", "422337", 142.50);
			Inventory.getInstance().getProductCatalog().addProduct("Ketchup", "941223", 3.50);
			
			showList(productCatalog.getAllProduct());
			
			Toast.makeText(getActivity().getBaseContext(), "products added.", Toast.LENGTH_SHORT).show();
			
		} catch (NoDaoSetException e) {
			e.printStackTrace();
		}
	}
	public void showPopup(View anchorView) {
		AddProductDialogFragment newFragment = new AddProductDialogFragment(InventoryFragment.this);
	    newFragment.show(getFragmentManager(), "dialog");
	}
	
	@Override
	public void update() {
		search();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
	}
	
}