# -*- coding: utf-8 -*-
"""
Created on Sun Jul 12 21:59:03 2015

@author: Phenomics
"""
# run LoanRiskAnalysis
from LoanRiskAnalysis import *

#%% to generate data matrix and label vector
import numpy as np
from sklearn import svm, metrics, linear_model
from sklearn.linear_model import SGDClassifier
from sklearn.ensemble import AdaBoostClassifier
from sklearn.tree import DecisionTreeClassifier
from sklearn.lda import LDA
from sklearn.qda import QDA
from sklearn.neighbors import KNeighborsClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.ensemble import ExtraTreesClassifier
from sklearn.ensemble import BaggingClassifier
from sklearn.preprocessing import normalize as arrNormalize
    
NN = 30
Perf_M = []
for k in range(NN):
    Perf_M.append([])
    # split data into training and testing
#%%
    training_perc, testing_perc = [0.8,0.2]
    rng = np.random.permutation(rows)
    idx_train =rng[:int(rows*training_perc)]
    idx_test=rng[int(rows*training_perc):]
    
    training_data,training_label = X_normalized[idx_train], Y[idx_train]
    testing_data,testing_label = X_normalized[idx_test], Y[idx_test]
    
    # %%
    classifiers=[
                svm.SVC(C=100, cache_size=200, class_weight=None, coef0=0.0, degree=3,
                         gamma=0.0, kernel='poly', max_iter=-1, probability=True, random_state=None,
                         shrinking=True, tol=0.001, verbose=False),
                linear_model.LogisticRegression(),
                LDA(),
                QDA(),
                AdaBoostClassifier(DecisionTreeClassifier(max_depth=5), n_estimators=400),
                KNeighborsClassifier(n_neighbors=30),
                BaggingClassifier(DecisionTreeClassifier(max_depth=5), n_estimators=400),
                RandomForestClassifier(max_depth=5, n_estimators=100),
                ExtraTreesClassifier(bootstrap=True, max_depth=5)
                ]
    
    
# training
    idx_clf = 0;
    classifier = classifiers[idx_clf]
    classifier.fit(training_data, training_label)
    
    #testing
    
    predicted_label_test = classifier.predict(testing_data)
    predicted_probability_test=classifier.predict_proba(testing_data)
    ConFusionMat_test=metrics.confusion_matrix(testing_label, predicted_label_test)
    ClfReport_test=metrics.classification_report(testing_label, predicted_label_test)
    accuracy = metrics.accuracy_score(testing_label, predicted_label_test)
#    print "\nClassifier:", classifier
#    print ConFusionMat_test
#    print ClfReport_test
#    print 'accuracy = %f'%accuracy
    
    # %% combination of classifiers
    combine_predicted_probability_test = np.zeros(predicted_probability_test.shape)
    for idx_clf in range(len(classifiers)):
        classifier = classifiers[idx_clf]
        classifier.fit(training_data, training_label)
        
        #testing
        predicted_label_test = classifier.predict(testing_data)
        predicted_probability_test = classifier.predict_proba(testing_data)
        accuracy = metrics.accuracy_score(testing_label, predicted_label_test)
        Perf_M[-1].append(accuracy)
        mean_acc = np.mean([a[idx_clf] for a in Perf_M])
        combine_predicted_probability_test += np.round(predicted_probability_test)*(mean_acc-0.55)
        ConFusionMat_test=metrics.confusion_matrix(testing_label, predicted_label_test)
        ClfReport_test=metrics.classification_report(testing_label, predicted_label_test)
        print '*'*60, "\nClassifier:", classifier
        print ConFusionMat_test
        print ClfReport_test
        print 'accuracy = %f'%accuracy
        
    
    combined_predicted_label_test = classifier.classes_[combine_predicted_probability_test.argmax(axis=1)]
    ConFusionMat_test=metrics.confusion_matrix(testing_label, combined_predicted_label_test)
    ClfReport_test=metrics.classification_report(testing_label, combined_predicted_label_test)
    accuracy = metrics.accuracy_score(testing_label, combined_predicted_label_test)
    print '*-'*30, '\n'*2 + "Combined Classifier:"
    print ConFusionMat_test
    print ClfReport_test
    print 'accuracy = %f'%accuracy
    Perf_M[-1].append(accuracy)

#%%
Perf_M1 = np.array(Perf_M)
for ir in range(Perf_M1.shape[0])[::-1]:
    Perf_M1[ir,:]=np.mean(Perf_M1[:ir+1,:],axis=0)
plt.figure(figsize=(8,5))    
colormap = plt.cm.gist_ncar
plt.gca().set_color_cycle([colormap(ic) for ic in np.linspace(0, 0.9, len(classifiers))])
markers=['*','+','x']*4
for  _s, _x in zip(markers, np.transpose(Perf_M1[:,:-1])):
    plt.plot(_x, marker = _s);

plt.plot(Perf_M1[:,-1],'r-o');
plt.axis([0, Perf_M1.shape[0], 0.5,0.75]);
plt.legend([str(clf).split('(')[0][:16] for clf in classifiers]+['combined'], 
            loc='right',fancybox=True, shadow=True,bbox_to_anchor=[1.35, 0.5]);
plt.title("Performance of classifiers")
plt.xlabel("index of test count")
plt.ylabel("mean accuracy")
plt.savefig('classifier_performance.png')






    
