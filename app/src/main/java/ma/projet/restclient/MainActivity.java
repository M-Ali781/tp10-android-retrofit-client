package ma.projet.restclient;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ma.projet.restclient.adapter.CompteAdapter;
import ma.projet.restclient.entities.Compte;
import ma.projet.restclient.repository.CompteRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements CompteAdapter.OnDeleteClickListener, CompteAdapter.OnUpdateClickListener {
    private RecyclerView recyclerView;
    private CompteAdapter adapter;
    private RadioGroup formatGroup;
    private FloatingActionButton addbtn;
    private String currentFormat = "JSON";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupFormatSelection();
        setupAddButton();

        loadData(currentFormat);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        formatGroup = findViewById(R.id.formatGroup);
        addbtn = findViewById(R.id.fabAdd);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompteAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupFormatSelection() {
        formatGroup.setOnCheckedChangeListener((group, checkedId) -> {
            currentFormat = (checkedId == R.id.radioJson) ? "JSON" : "XML";
            loadData(currentFormat);
        });
    }

    private void setupAddButton() {
        addbtn.setOnClickListener(v -> showAddCompteDialog());
    }

    private void showAddCompteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);

        builder.setView(dialogView)
            .setTitle(getString(R.string.add_compte))
            .setPositiveButton(getString(R.string.btn_add), (dialog, which) -> {
                String soldeStr = etSolde.getText().toString().trim();
                if (soldeStr.isEmpty()) {
                    showToast("Veuillez saisir un solde valide");
                    return;
                }
                double solde;
                try {
                    solde = Double.parseDouble(soldeStr);
                } catch (NumberFormatException e) {
                    showToast("Format de solde invalide");
                    return;
                }

                String type = (typeGroup.getCheckedRadioButtonId() == R.id.radioCourant)
                    ? "COURANT" : "EPARGNE";

                String formattedDate = getCurrentDateFormatted();
                Compte compte = new Compte(null, solde, type, formattedDate);
                addCompte(compte);
            })
            .setNegativeButton(getString(R.string.btn_cancel), null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private String getCurrentDateFormatted() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return formatter.format(calendar.getTime());
    }

    private void addCompte(Compte compte) {
        CompteRepository compteRepository = new CompteRepository(currentFormat);
        compteRepository.addCompte(compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    showToast("Compte ajouté");
                    loadData(currentFormat);
                } else {
                    showToast("Erreur serveur : " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                showToast("Erreur lors de l'ajout: " + t.getMessage());
            }
        });
    }

    private void loadData(String format) {
        CompteRepository compteRepository = new CompteRepository(format);
        compteRepository.getAllCompte(new Callback<List<Compte>>() {
            @Override
            public void onResponse(Call<List<Compte>> call, Response<List<Compte>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Compte> comptes = response.body();
                    runOnUiThread(() -> adapter.updateData(comptes));
                } else {
                    showToast("Erreur lors du chargement (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<Compte>> call, Throwable t) {
                showToast("Erreur réseau: " + t.getMessage());
            }
        });
    }

    @Override
    public void onUpdateClick(Compte compte) {
        showUpdateCompteDialog(compte);
    }

    private void showUpdateCompteDialog(Compte compte) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_compte, null);

        EditText etSolde = dialogView.findViewById(R.id.etSolde);
        RadioGroup typeGroup = dialogView.findViewById(R.id.typeGroup);
        etSolde.setText(String.valueOf(compte.getSolde()));

        if (compte.getType() != null && compte.getType().equalsIgnoreCase("COURANT")) {
            typeGroup.check(R.id.radioCourant);
        } else if (compte.getType() != null && compte.getType().equalsIgnoreCase("EPARGNE")) {
            typeGroup.check(R.id.radioEpargne);
        }

        builder.setView(dialogView)
            .setTitle(getString(R.string.edit_compte))
            .setPositiveButton(getString(R.string.btn_edit), (dialog, which) -> {
                String soldeStr = etSolde.getText().toString().trim();
                if (soldeStr.isEmpty()) {
                    showToast("Veuillez saisir un solde");
                    return;
                }
                double solde;
                try {
                    solde = Double.parseDouble(soldeStr);
                } catch (NumberFormatException e) {
                    showToast("Format de solde invalide");
                    return;
                }
                String type = (typeGroup.getCheckedRadioButtonId() == R.id.radioCourant)
                    ? "COURANT" : "EPARGNE";
                compte.setSolde(solde);
                compte.setType(type);
                updateCompte(compte);
            })
            .setNegativeButton(getString(R.string.btn_cancel), null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateCompte(Compte compte) {
        CompteRepository compteRepository = new CompteRepository(currentFormat);
        compteRepository.updateCompte(compte.getId(), compte, new Callback<Compte>() {
            @Override
            public void onResponse(Call<Compte> call, Response<Compte> response) {
                if (response.isSuccessful()) {
                    showToast("Compte modifié");
                    loadData(currentFormat);
                } else {
                    showToast("Erreur lors de la modification : " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Compte> call, Throwable t) {
                showToast("Erreur lors de la modification: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDeleteClick(Compte compte) {
        showDeleteConfirmationDialog(compte);
    }

    private void showDeleteConfirmationDialog(Compte compte) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_title))
            .setMessage(getString(R.string.confirm_delete_msg))
            .setPositiveButton(getString(R.string.yes), (dialog, which) -> deleteCompte(compte))
            .setNegativeButton(getString(R.string.no), null)
            .show();
    }

    private void deleteCompte(Compte compte) {
        CompteRepository compteRepository = new CompteRepository(currentFormat);
        compteRepository.deleteCompte(compte.getId(), new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showToast("Compte supprimé");
                    loadData(currentFormat);
                } else {
                    showToast("Erreur lors de la suppression : " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showToast("Erreur lors de la suppression: " + t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show());
    }
}

